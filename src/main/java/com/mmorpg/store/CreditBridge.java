package com.mmorpg.store;
import com.mmorpg.MMORPG;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/** Local-only integration endpoint for the website backend (backend/ in this repository).
 *  POST /credits : the backend has verified a payment and asks for its credit pack (idempotent per payment reference).
 *  POST /balance : the backend reads a player's credit balance for the website and launcher.
 *  Every request is signed with HMAC-SHA256 (AELORIA_CREDIT_SECRET) and expires after 5 minutes. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CreditBridge {
 private static HttpServer bridge;
 interface Work{Map<String,Object> run(JsonObject json)throws Exception;}
 public static boolean validSignature(byte[] body,String signature,String secret){try{
  if(secret==null||secret.length()<32||signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;
  var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return MessageDigest.isEqual(mac.doFinal(body),HexFormat.of().parseHex(signature));
 }catch(Exception e){return false;}}
 /** Runs ledger work on Minecraft's server thread and waits for it (bounded). */
 private static <T>T onServer(MinecraftServer server,java.util.concurrent.Callable<T> work)throws Exception{
  var result=new CompletableFuture<T>();server.execute(()->{try{result.complete(work.call());}catch(Throwable ex){result.completeExceptionally(ex);}});
  return result.get(10,TimeUnit.SECONDS);
 }
 private static void endpoint(HttpServer bridge,String path,String secret,Work work){
  bridge.createContext(path,exchange->{int status=400;String message="Requête invalide";Map<String,Object> extra=Map.of();
   try{
    if(!exchange.getRequestMethod().equals("POST")){status=405;throw new IllegalArgumentException("POST requis");}
    byte[] body=exchange.getRequestBody().readNBytes(8193);if(body.length>8192){status=413;throw new IllegalArgumentException("Requête trop grande");}
    if(!validSignature(body,exchange.getRequestHeaders().getFirst("X-Aeloria-Signature"),secret)){status=401;throw new IllegalArgumentException("Signature invalide");}
    var json=JsonParser.parseString(new String(body,StandardCharsets.UTF_8)).getAsJsonObject();long timestamp=json.get("timestamp").getAsLong();
    if(Math.abs(System.currentTimeMillis()/1000-timestamp)>300){status=401;throw new IllegalArgumentException("Signature expirée");}
    extra=work.run(json);status=200;message=String.valueOf(extra.getOrDefault("message","OK"));
   }catch(Exception ex){message=status==400?"Requête refusée : "+(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()):ex.getMessage();}
   var reply=new LinkedHashMap<String,Object>();reply.put("ok",status==200);reply.put("message",message==null?"Erreur":message);
   if(status==200)extra.forEach((k,v)->{if(!k.equals("message"))reply.put(k,v);});
   byte[] output=new Gson().toJson(reply).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(status,output.length);try(var out=exchange.getResponseBody()){out.write(output);}
  });
 }
 @SubscribeEvent public static void start(net.neoforged.neoforge.event.server.ServerStartedEvent e){
  if(!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN))return;
  var cfg=StoreConfig.load(e.getServer());if(!cfg.paymentBridgeEnabled)return;
  String secret=System.getenv("AELORIA_CREDIT_SECRET");if(secret==null||secret.length()<32)throw new IllegalStateException("La passerelle crédits nécessite AELORIA_CREDIT_SECRET (32 caractères minimum).");
  var server=e.getServer();
  try{bridge=HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",cfg.paymentBridgePort),4);
   // No commands or client-supplied credit amounts are executed: the pack amount always comes from boutique.json.
   endpoint(bridge,"/credits",secret,json->{
    UUID player=UUID.fromString(json.get("player").getAsString());String pack=json.get("pack").getAsString(),payment=json.get("payment").getAsString();
    Long amount=StoreConfig.load(server).creditPacks.get(pack);if(amount==null)throw new IllegalArgumentException("Pack inconnu");
    onServer(server,()->{StoreLedger.credit(server,player,amount,"web:"+payment);return null;});
    return Map.of("message","Crédits enregistrés","credits",amount);
   });
   endpoint(bridge,"/balance",secret,json->{
    UUID player=UUID.fromString(json.get("player").getAsString());
    long credits=onServer(server,()->StoreLedger.read(server,s->{var a=s.accounts.get(player.toString());return a==null?0L:a.credits;}));
    return Map.of("message","Solde","credits",credits);
   });
   bridge.start();MMORPG.LOGGER.info("Passerelle crédits active uniquement sur loopback, port {}",cfg.paymentBridgePort);
  }catch(java.io.IOException ex){throw new IllegalStateException("Passerelle crédits indisponible",ex);}
 }
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppingEvent e){if(bridge!=null){bridge.stop(0);bridge=null;}}
}
