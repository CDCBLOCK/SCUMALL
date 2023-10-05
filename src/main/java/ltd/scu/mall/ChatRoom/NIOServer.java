package ltd.scu.mall.ChatRoom;

import org.apache.ibatis.annotations.SelectKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {

    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int Port = 6667;

    public NIOServer(){
        try{
            selector = Selector.open();
            listenChannel = ServerSocketChannel.open();
            listenChannel.socket().bind(new InetSocketAddress(Port));
            listenChannel.configureBlocking(false);
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void listen(){
        try{
            while(true){
//                System.out.println("wait for connection");
                int count = selector.select();
                if(count > 0){
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        if(key.isAcceptable()) {
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            System.out.println(sc.getRemoteAddress() + " is online now!");
                        }

                        if(key.isReadable()) {
                            //处里读
                            readData(key);
                        }
                        iterator.remove();
                    }

                }
                else{
                    System.out.println("waiting!!!");
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void readData(SelectionKey key){
        SocketChannel channel = null;
        try {
            channel = (SocketChannel)key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);
            if(count > 0){
                String msg = new String(buffer.array());
                System.out.println("from Client " + msg);
                //向其他客户端转发这条消息
                sendToOther(msg, channel);
            }



        } catch (IOException e) {
            try{
                System.out.println(channel.getRemoteAddress() + "离线");
                //取消注册
                key.cancel();
                //关闭通道
                channel.close();
            }catch (IOException e2){
                e.printStackTrace();
            }

        }


    }

    //转发消息给替他客户端，就是发给其他的通道
    public void sendToOther(String msg, SocketChannel self) throws IOException {
        System.out.println("服务器转发消息ing...");
        for(SelectionKey key: selector.keys()){
            Channel targerchannel = key.channel();
            //排除自己
            if(targerchannel instanceof SocketChannel && targerchannel != self){
                SocketChannel dest = (SocketChannel)targerchannel;
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                //写入通道
                dest.write(buffer);

            }
        }
    }

    public static void main(String[] args) throws Exception {
        NIOServer nioServer = new NIOServer();
        nioServer.listen();

    }
}
