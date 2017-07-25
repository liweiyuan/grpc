/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc.examples.routeguide;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideBlockingStub;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideStub;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class RouteGuideClient {
  private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());

  private final ManagedChannel channel;
  private final RouteGuideBlockingStub blockingStub;
  private final RouteGuideStub asyncStub;

  private Random random = new Random();
  private TestHelper testHelper;

  /** Construct client for accessing RouteGuide server at {@code host:port}. */
  public RouteGuideClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
    channel = channelBuilder.build();
    blockingStub = RouteGuideGrpc.newBlockingStub(channel);
    asyncStub = RouteGuideGrpc.newStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Blocking unary call example.  Calls getFeature and prints the response.
   */
  public void getFeature(int lat, int lon) {
    info("*** GetFeature: lat={0} lon={1}", lat, lon);

    Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

    Feature feature;
    
   
    try {
      feature = blockingStub.getFeature(request);
      if (testHelper != null) {
        testHelper.onMessage(feature);
      }
    } catch (StatusRuntimeException e) {
      warning("RPC failed: {0}", e.getStatus());
      if (testHelper != null) {
        testHelper.onRpcError(e);
      }
      return;
    }
    if (RouteGuideUtil.exists(feature)) {
      info("Found feature called \"{0}\" at {1}, {2}",
          feature.getName(),
          RouteGuideUtil.getLatitude(feature.getLocation()),
          RouteGuideUtil.getLongitude(feature.getLocation()));
    } else {
      info("Found no feature at {0}, {1}",
          RouteGuideUtil.getLatitude(feature.getLocation()),
          RouteGuideUtil.getLongitude(feature.getLocation()));
    }
  }

  /**
   * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
   * response feature as it arrives.
   */
  public void listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
    info("*** ListFeatures: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat,
        hiLon);

    Rectangle request =
        Rectangle.newBuilder()
            .setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
            .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build()).build();
    Iterator<Feature> features;
    
   
    try {
      features = blockingStub.listFeatures(request);
      for (int i = 1; features.hasNext(); i++) {
        Feature feature = features.next();
        info("Result #" + i + ": {0}", feature);
        if (testHelper != null) {
          testHelper.onMessage(feature);
        }
      }
    } catch (StatusRuntimeException e) {
      warning("RPC failed: {0}", e.getStatus());
      if (testHelper != null) {
        testHelper.onRpcError(e);
      }
    }
  }



  /**
   * Bi-directional example, which can only be asynchronous. Send some chat messages, and print any
   * chat messages that are sent from the server.
   */
  public CountDownLatch routeChat() {
    info("*** RouteChat");
    final CountDownLatch finishLatch = new CountDownLatch(1);
    
   
    StreamObserver<RouteNote> requestObserver =
        asyncStub.routeChat(new StreamObserver<RouteNote>() {
          @Override
          public void onNext(RouteNote note) {
            info("Got message \"{0}\" at {1}, {2}", note.getMessage(), note.getLocation()
                .getLatitude(), note.getLocation().getLongitude());
            if (testHelper != null) {
              testHelper.onMessage(note);
            }
          }

          @Override
          public void onError(Throwable t) {
            warning("RouteChat Failed: {0}", Status.fromThrowable(t));
            if (testHelper != null) {
              testHelper.onRpcError(t);
            }
            finishLatch.countDown();
          }

          @Override
          public void onCompleted() {
            info("Finished RouteChat");
            finishLatch.countDown();
          }
        });

    try {
      RouteNote[] requests =
          {newNote("First message", 0, 0), newNote("Second message", 0, 1),
              newNote("Third message", 1, 0), newNote("Fourth message", 1, 1)};

      for (RouteNote request : requests) {
        info("Sending message \"{0}\" at {1}, {2}", request.getMessage(), request.getLocation()
            .getLatitude(), request.getLocation().getLongitude());
        requestObserver.onNext(request);
      }
    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }
    // Mark the end of requests
    requestObserver.onCompleted();

    // return the latch while receiving happens asynchronously
    return finishLatch;
  }

  /** Issues several different requests and then exits. */
//  public static void main(String[] args) throws InterruptedException {
//    List<Feature> features;
//    try {
//      features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
//    } catch (IOException ex) {
//      ex.printStackTrace();
//      return;
//    }
//
//    RouteGuideClient client = new RouteGuideClient("localhost", 8980);
//    try {
//      // Looking for a valid feature
//      client.getFeature(409146138, -746188906);
//
//      // Feature missing.
//      client.getFeature(0, 0);
//
//      // Looking for features between 40, -75 and 42, -73.
//      client.listFeatures(400000000, -750000000, 420000000, -730000000);
//
//      // Record a few randomly selected points from the features file.
//      client.recordRoute(features, 10);
//
//      // Send and receive some notes.
//      CountDownLatch finishLatch = client.routeChat();
//
//      if (!finishLatch.await(1, TimeUnit.MINUTES)) {
//        client.warning("routeChat can not finish within 1 minutes");
//      }
//    } finally {
//      client.shutdown();
//    }
//  }

  private void info(String msg, Object... params) {
    logger.log(Level.INFO, msg, params);
  }

  public void warning(String msg, Object... params) {
    logger.log(Level.WARNING, msg, params);
  }

  private RouteNote newNote(String message, int lat, int lon) {
    return RouteNote.newBuilder().setMessage(message)
        .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
  }

  /**
   * Only used for unit test, as we do not want to introduce randomness in unit test.
   */
  @VisibleForTesting
  void setRandom(Random random) {
    this.random = random;
  }

  /**
   * Only used for helping unit test.
   */
  @VisibleForTesting
  interface TestHelper {
    /**
     * Used for verify/inspect message received from server.
     */
    void onMessage(Message message);

    /**
     * Used for verify/inspect error received from server.
     */
    void onRpcError(Throwable exception);
  }

  @VisibleForTesting
  void setTestHelper(TestHelper testHelper) {
    this.testHelper = testHelper;
  }
}