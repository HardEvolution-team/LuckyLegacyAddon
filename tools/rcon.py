import socket,struct,sys
def rcon(cmd,port=25578,pw="test"):
    s=socket.create_connection(("127.0.0.1",port),timeout=15)
    def send(i,t,b):
        d=struct.pack("<ii",i,t)+b.encode()+b"\0\0"; s.sendall(struct.pack("<i",len(d))+d)
    def recv():
        n=struct.unpack("<i",s.recv(4))[0]; d=b""
        while len(d)<n: d+=s.recv(n-len(d))
        return d[8:-2].decode(errors="replace")
    send(1,3,pw); recv(); recv() if False else None
    send(2,2,cmd)
    try: r=recv()
    except Exception: r=""
    s.close(); return r
if __name__=="__main__": print(rcon(" ".join(sys.argv[1:])))
