/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package docs.jrouting;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import akka.testkit.JavaTestKit;
import akka.actor.ActorSystem;

//#imports1
import akka.actor.UntypedActor;
import akka.routing.ConsistentHashingRouter.ConsistentHashable;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
//#imports1

//#imports2
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.routing.ConsistentHashingRouter;
import akka.routing.ConsistentHashingRouter.ConsistentHashMapper;
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope;
//#imports2

public class ConsistentHashingRouterDocTestBase {

  static ActorSystem system;
  
  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }
  
  @AfterClass
  public static void teardown() {
    system.shutdown();
  }

  static
  //#cache-actor
  public class Cache extends UntypedActor {
    Map<String, String> cache = new HashMap<String, String>();

    public void onReceive(Object msg) {
      if (msg instanceof Entry) {
        Entry entry = (Entry) msg;
        cache.put(entry.key, entry.value);
      } else if (msg instanceof Get) {
        Get get = (Get) msg;
        Object value = cache.get(get.key);
        getSender().tell(value == null ? NOT_FOUND : value, 
          getContext().self());
      } else if (msg instanceof Evict) {
        Evict evict = (Evict) msg;
        cache.remove(evict.key);
      } else {
        unhandled(msg);
      }
    }
  }

  //#cache-actor
  static
  //#cache-actor
  public final class Evict implements Serializable {
    public final String key;
    public Evict(String key) {
      this.key = key;
    }
  }

  //#cache-actor
  static
  //#cache-actor
  public final class Get implements Serializable, ConsistentHashable {
    public final String key;
    public Get(String key) {
      this.key = key;
    }
    public Object consistentHashKey() {
      return key;
    } 
  }

  //#cache-actor
  static
  //#cache-actor
  public final class Entry implements Serializable {
    public final String key;
    public final String value;
    public Entry(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  //#cache-actor
  static
  //#cache-actor
  public final String NOT_FOUND = "NOT_FOUND";
  //#cache-actor  


  @Test
  public void demonstrateUsageOfConsistentHashableRouter() {

    new JavaTestKit(system) {{

      //#consistent-hashing-router      
      
      final ConsistentHashMapper hashMapper = new ConsistentHashMapper() {
        @Override
        public Object hashKey(Object message) {
          if (message instanceof Evict) {
            return ((Evict) message).key;
          } else {
            return null;
          }
        }
      };

      ActorRef cache = system.actorOf(new Props(Cache.class).withRouter(
        new ConsistentHashingRouter(10).withHashMapper(hashMapper)), 
        "cache");

      cache.tell(new ConsistentHashableEnvelope(
        new Entry("hello", "HELLO"), "hello"), getRef());
      cache.tell(new ConsistentHashableEnvelope(
        new Entry("hi", "HI"), "hi"), getRef());

      cache.tell(new Get("hello"), getRef());
      expectMsgEquals("HELLO");

      cache.tell(new Get("hi"), getRef());
      expectMsgEquals("HI");

      cache.tell(new Evict("hi"), getRef());
      cache.tell(new Get("hi"), getRef());
      expectMsgEquals(NOT_FOUND);

      //#consistent-hashing-router
    }};
  }

}
