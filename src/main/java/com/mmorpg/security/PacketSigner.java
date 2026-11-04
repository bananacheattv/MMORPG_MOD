package com.mmorpg.security;

import com.mmorpg.MMORPG;
import com.mmorpg.anticheat.AntiCheat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Signature des paquets sensibles. A la connexion le serveur remet au client une cle de session ; avant chaque paquet sensible le client
 * envoie une signature HMAC-SHA256 (numero croissant + nom + contenu). Un paquet forge, rejoue ou modifie en route est refuse. Cela protege
 * contre l'injection et le rejeu, pas contre un client modifie qui connait la cle (d'ou les autres controles du serveur).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PacketSigner {
    private PacketSigner() {
    }

    public record SessionKey(byte[] key) implements CustomPacketPayload {
        public static final Type<SessionKey> TYPE = new Type<>(MMORPG.id("session_key"));
        public static final StreamCodec<ByteBuf, SessionKey> CODEC = StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, SessionKey::key, SessionKey::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Sign(int seq, byte[] mac) implements CustomPacketPayload {
        public static final Type<Sign> TYPE = new Type<>(MMORPG.id("packet_sign"));
        public static final StreamCodec<ByteBuf, Sign> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Sign::seq, ByteBufCodecs.BYTE_ARRAY, Sign::mac, Sign::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Paquets signes (identifiant sans espace de noms). */
    public static final Set<String> SIGNED = Set.of("admin_request", "social_action", "forge_action", "shop_action", "service_action",
            "quest_edit", "loot_edit", "merchant_edit", "teleporter_setup", "dungeon_setup", "hub_action");
    /** Delai apres la remise de la cle pendant lequel un paquet non signe est tolere (le client n'a pas encore recu la cle). */
    static final long GRACE_MS = 5000;

    private static final SecureRandom RNG = new SecureRandom();
    private static final Map<String, StreamCodec<? super RegistryFriendlyByteBuf, ?>> CODECS = new ConcurrentHashMap<>();

    private static final class State {
        final byte[] key = new byte[32];
        final long since = System.currentTimeMillis();
        int last;
        Sign pending;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static void register(String name, StreamCodec<? super RegistryFriendlyByteBuf, ?> codec) {
        CODECS.put(name, codec);
    }

    // ---------------------------------------------------------------- serveur

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !AntiCheat.on(p)) return;
        State s = new State();
        RNG.nextBytes(s.key);
        STATES.put(p.getUUID(), s);
        com.mmorpg.network.Net.toPlayer(p, new SessionKey(s.key.clone()));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        STATES.remove(e.getEntity().getUUID());
        ScreenGuard.forget(e.getEntity().getUUID());
    }

    public static void received(ServerPlayer p, Sign sign) {
        State s = STATES.get(p.getUUID());
        if (s != null) s.pending = sign;
    }

    /** Vrai si le paquet n'a pas besoin de signature ou si sa signature est valide. */
    public static boolean verify(ServerPlayer p, String name, CustomPacketPayload packet) {
        if (!SIGNED.contains(name)) return true;
        State s = STATES.get(p.getUUID());
        if (s == null) return true;
        Sign sign = s.pending;
        s.pending = null;
        if (sign == null) {
            if (System.currentTimeMillis() - s.since < GRACE_MS) return true;
            AntiCheat.flag(p, AntiCheat.Check.SIGNATURE, name + " : non signé");
            return false;
        }
        byte[] body = encode(name, packet, p.registryAccess());
        if (body == null || sign.seq() <= s.last || !check(s.key, sign.seq(), name, body, sign.mac())) {
            AntiCheat.flag(p, AntiCheat.Check.SIGNATURE, name + " : " + (sign.seq() <= s.last ? "rejeu" : "signature fausse"));
            return false;
        }
        if (s.last == 0) MMORPG.LOGGER.info("[MMORPG] Signature de paquets : premier paquet signé valide de {} ({})", p.getGameProfile().name(), name);
        s.last = sign.seq();
        return true;
    }

    /** Pour les tests : etat de signature d'un joueur. */
    public static byte[] keyOf(UUID id) {
        State s = STATES.get(id);
        return s == null ? null : s.key.clone();
    }

    // ---------------------------------------------------------------- commun

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static byte[] encode(String name, CustomPacketPayload packet, net.minecraft.core.RegistryAccess access) {
        StreamCodec codec = CODECS.get(name);
        if (codec == null) return null;
        try {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
            codec.encode(buf, packet);
            // relecture : les deux cotes signent la forme obtenue apres un aller-retour, independante de l'ordre d'insertion des etiquettes
            Object again = codec.decode(buf);
            RegistryFriendlyByteBuf out = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
            codec.encode(out, again);
            byte[] bytes = new byte[out.readableBytes()];
            out.readBytes(bytes);
            return bytes;
        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] mac(byte[] key, int seq, String name, byte[] body) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key, "HmacSHA256"));
            m.update(new byte[]{(byte) (seq >>> 24), (byte) (seq >>> 16), (byte) (seq >>> 8), (byte) seq});
            m.update(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            m.update((byte) 0);
            m.update(body);
            return m.doFinal();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static boolean check(byte[] key, int seq, String name, byte[] body, byte[] mac) {
        return MessageDigest.isEqual(mac(key, seq, name, body), mac);
    }

    // ---------------------------------------------------------------- client

    private static volatile byte[] clientKey;
    private static int clientSeq;

    public static synchronized void clientKey(byte[] key) {
        clientKey = key;
        clientSeq = 0;
    }

    /** Envoie la signature qui precede un paquet sensible (appele par {@code ClientNet.send}). */
    public static synchronized void signBeforeSend(CustomPacketPayload payload, net.minecraft.core.RegistryAccess access, Consumer<CustomPacketPayload> sender) {
        byte[] key = clientKey;
        String name = payload.type().id().getPath();
        if (key == null || !SIGNED.contains(name)) return;
        if (access == null) return;
        byte[] body = encode(name, payload, access);
        if (body == null) return;
        int seq = ++clientSeq;
        sender.accept(new Sign(seq, mac(key, seq, name, body)));
    }
}
