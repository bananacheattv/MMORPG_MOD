from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import struct,zlib,json,base64

def png(height):
 def chunk(k,v): return struct.pack(">I",len(v))+k+v+struct.pack(">I",zlib.crc32(k+v)&0xffffffff)
 rows=b"".join(b"\x00"+b"".join(bytes((170 if (x//8+y//8)%2 else 70,80,220,255)) for x in range(64)) for y in range(height))
 return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",64,height,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(rows))+chunk(b"IEND",b"")
class Handler(BaseHTTPRequestHandler):
 flaky=0
 def do_GET(self):
  status=200;typ="application/json"
  if self.path.startswith("/users/profiles/minecraft/"):body=json.dumps({"id":"qaslim"}).encode()
  elif self.path.startswith("/session/minecraft/profile/"):
   value=base64.b64encode(json.dumps({"textures":{"SKIN":{"url":"http://127.0.0.1:8765/skin.png","metadata":{"model":"slim"}}}}).encode()).decode()
   body=json.dumps({"properties":[{"name":"textures","value":value}]}).encode()
  elif self.path in ("/skin.png","/legacy.png","/flaky.png"):
   typ="image/png";body=png(32 if self.path=="/legacy.png" else 64)
   if self.path=="/flaky.png":
    Handler.flaky+=1
    if Handler.flaky==1:status=503;body=b"Retry test"
  else:status=404;body=b"Missing"
  self.send_response(status);self.send_header("Content-Type",typ);self.send_header("Content-Length",str(len(body)));self.end_headers();self.wfile.write(body)
 def log_message(self,fmt,*args):print(fmt%args,flush=True)
ThreadingHTTPServer(("127.0.0.1",8765),Handler).serve_forever()
