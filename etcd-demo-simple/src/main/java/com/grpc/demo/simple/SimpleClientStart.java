package com.grpc.demo.simple;


import com.coreos.jetcd.api.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class SimpleClientStart {

    private ManagedChannel managedChannel;
    private int PORT = 2379;

    private void createChannel() {
        managedChannel = NettyChannelBuilder.forAddress("localhost", PORT).usePlaintext(true).build();
    }

    private void shutdown() {
        if (managedChannel != null) {
            try {
                managedChannel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SimpleClientStart simpleClientStart = new SimpleClientStart();
        simpleClientStart.createChannel();
        String key ="yyc\\test";
        WatchGrpc.WatchStub registryClient = WatchGrpc.newStub(simpleClientStart.managedChannel);
        KVGrpc.KVBlockingStub kvBlockingStub = KVGrpc.newBlockingStub(simpleClientStart.managedChannel);
        RangeResponse range = kvBlockingStub.range(RangeRequest.newBuilder().setKey(ByteString.copyFromUtf8(key)).build());

        StreamObserver<WatchRequest> requestStreamObserver = registryClient.watch(new StreamObserver<WatchResponse>() {
            @Override
            public void onNext(WatchResponse value) {
                System.out.println(value.toString());
                System.out.println("有变化------------------");
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("完成");
            }
        });

        WatchRequest watchRequest = WatchRequest.newBuilder().setCreateRequest(WatchCreateRequest.newBuilder().setKey(ByteString.copyFromUtf8(key)).build()).build();
        requestStreamObserver.onNext(watchRequest);


        TimeUnit.HOURS.sleep(1);
    }
}
