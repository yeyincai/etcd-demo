package com.grpc.demo.simple;


import com.coreos.jetcd.api.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeyputStart {

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
        KeyputStart simpleClientStart = new KeyputStart();
        simpleClientStart.createChannel();

        KVGrpc.KVBlockingStub kvBlockingStub = KVGrpc.newBlockingStub(simpleClientStart.managedChannel);
        LeaseGrpc.LeaseBlockingStub leaseBlocking = LeaseGrpc.newBlockingStub(simpleClientStart.managedChannel);

        LeaseGrpc.LeaseStub leaseStub = LeaseGrpc.newStub(simpleClientStart.managedChannel);

        String key ="yyc\\test";
        String value ="tototototoototto";
        LeaseGrantResponse leaseGrantResponse = leaseBlocking.leaseGrant(LeaseGrantRequest.newBuilder().setTTL(5l).build());
        long leaseId = leaseGrantResponse.getID();

        PutResponse put = kvBlockingStub.put(PutRequest.newBuilder().setKey(ByteString.copyFromUtf8(key)).setValue(ByteString.copyFromUtf8(value)).setLease(leaseId).build());

        StreamObserver<LeaseKeepAliveRequest> leaseKeepAliveRequestStreamObserver = leaseStub.leaseKeepAlive(new StreamObserver<LeaseKeepAliveResponse>() {

            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                System.out.println("LeaseKeepAliveResponse  value"+value);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted  !!!");
            }
        });

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(()->{
            leaseKeepAliveRequestStreamObserver.onNext(LeaseKeepAliveRequest.newBuilder().setID(leaseId).build());
        },1l,1l,TimeUnit.SECONDS);

    }
}
