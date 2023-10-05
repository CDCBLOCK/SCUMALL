package ltd.scu.mall.ChatRoom;

import org.apache.ibatis.annotations.SelectKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class NIOClient {
    private final String Host =  "127.0.0.1";
    private SocketChannel socketChannel;
    private Selector selector;
    private String username;
    private static final int Port = 6667;

    public NIOClient(){
        try{
            selector = Selector.open();
            socketChannel = SocketChannel.open(new InetSocketAddress(Host, Port));
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            username = socketChannel.getLocalAddress().toString().substring(1);
            System.out.println(username + " is ok!");


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void sendInfo(String info){
        info = username + " say:" + info;
        try{
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void readInfo(){

        try {
            int readChannels = selector.select();
            if(readChannels > 0){
                //用可用的通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if(key.isReadable()){
                    //得到相关通道
                        SocketChannel sc = (SocketChannel)key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        sc.read(buffer);
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                    iterator.remove();
                }


            }

            else {
                System.out.println("无可用通道！");
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        //启动客户端
        NIOClient nioClient = new NIOClient();
        //启动线程
        new Thread(){
            public void run(){
                while(true){
                    nioClient.readInfo();
                    try{
                        Thread.currentThread().sleep(3000);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()) {
        String s = scanner.nextLine();
        nioClient.sendInfo(s);
        }
    }


}
