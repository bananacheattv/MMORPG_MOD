package com.mmorpg.store;
import com.mmorpg.MMORPG;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/** Local-only integration endpoint. The future payment backend verifies payment before signing this request. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CreditBridge {
 private static HttpServer bridge;
 public static boolean validSignature(byte[] body,String signature,String secret){try{
  if(secret==null||secret.length()<32||signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;
  var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return MessageDigest.isEqual(mac.doFinal(body),HexFormat.of().parseHex(signature));
 }catch(Exception e){return false;}}
 @SubscribeEvent public static void start(net.neoforged.neoforge.event.server.ServerStartedEvent e){
  if(!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN))return;
  var cfg=StoreConfig.load(e.getServer());if(!cfg.paymentBridgeEnabled)return;
  String secret=System.getenv("AELORIA_CREDIT_SECRET");if(secret==null||secret.length()<32)throw new IllegalStateException("La passerelle crédits nécessite AELORIA_CREDIT_SECRET (32 caractères minimum).");
  try{bridge=HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",cfg.paymentBridgePort),4);
   bridge.createContext("/credits",exchange->{int status=400;String message="Requête invalide";
    try{
     if(!exchange.getRequestMethod().equals("POST")){status=405;throw new IllegalArgumentException("POST requis");}
     byte[] body=exchange.getRequestBody().readNBytes(8193);if(body.length>8192){status=413;throw new IllegalArgumentException("Requête trop grande");}
     if(!validSignature(body,exchange.getRequestHeaders().getFirst("X-Aeloria-Signature"),secret)){status=401;throw new IllegalArgumentException("Signature invalide");}
     var json=JsonParser.parseString(new String(body,StandardCharsets.UTF_8)).getAsJsonObject();long timestamp=json.get("timestamp").getAsLong();
     if(Math.abs(System.currentTimeMillis()/1000-timestamp)>300){status=401;throw new IllegalArgumentException("Signature expirée");}
     UUID player=UUID.fromString(json.get("player").getAsString());String pack=json.get("pack").getAsString(),payment=json.get("payment").getAsString();
     Long amount=StoreConfig.load(e.getServer()).creditPacks.get(pack);if(amount==null)throw new IllegalArgumentException("Pack inconnu");
     // Marshal mutations to Minecraft's server thread. No commands or client-supplied credit amounts are executed.
     var result=new java.util.concurrent.CompletableFuture<Void>();e.getServer().execute(()->{try{StoreLedger.credit(e.getServer(),player,amount,"web:"+payment);result.complete(null);}catch(Throwable ex){result.completeExceptionally(ex);}});
     result.get(10,java.util.concurrent.TimeUnit.SECONDS);status=200;message="Crédits enregistrés";
    }catch(Exception ex){message=status==400?"Requête refusée : "+(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()):ex.getMessage();}
    byte[] output=new Gson().toJson(Map.of("ok",status==200,"message",message==null?"Erreur":message)).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(status,output.length);try(var out=exchange.getResponseBody()){out.write(output);}
   });bridge.start();MMORPG.LOGGER.info("Passerelle crédits active uniquement sur loopback, port {}",cfg.paymentBridgePort);
  }catch(java.io.IOException ex){throw new IllegalStateException("Passerelle crédits indisponible",ex);}
 }
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppingEvent e){if(bridge!=null){bridge.stop(0);bridge=null;}}
}
