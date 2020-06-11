import io.reactivex.Observable;
import org.junit.Test;

import javax.sound.midi.Soundbank;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoup
 */
public class MainTest {

    @Test
    //无限数据
    public void infinite_test() {
        Observable<Object> observable = Observable.create(p -> {
            BigInteger in = BigInteger.ZERO;
            while (true) {
                p.onNext(in);
                in = in.add(BigInteger.ONE);
            }
        });
        //一直运作阻塞其他订阅事件
        observable.subscribe(p -> System.out.println(p));
    }

    //采用线程调度
    @Test
    public void infinite_thread_test() {
        Observable<Object> observable = Observable.create(p -> {
            Runnable runnable = () -> {
               BigInteger in = BigInteger.ZERO;
               while(true) {
                   p.onNext(in);
                   in = in.add(BigInteger.ONE);
               }
            };
            new Thread(runnable).start();
        });
        observable.subscribe(x -> System.out.println(x));
        observable.subscribe(x -> System.out.println("**" + x));
        try {
            Thread.sleep(50000);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void pool_publishCol(){
        List<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(3);
        integers.add(5);
        integers.add(7);
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        try{
            pushCol(integers, forkJoinPool).subscribe(x -> System.out.println("first" + x));
            pushCol(integers, forkJoinPool).subscribe(x -> System.out.println("second" + x));
        } catch (Exception e) {

        } finally {
            try {
                forkJoinPool.shutdown();
                int shutNum = 2;
                System.out.println("......  " + shutNum + "秒  ending");
                forkJoinPool.awaitTermination(shutNum, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("catch 到异常  : " + e.getClass().getName());
            } finally {
                System.out.println("调用结束  ！！！！");
                List<Runnable> shutdownNow = forkJoinPool.shutdownNow();
                System.out.println("还剩下 " + shutdownNow.size() + "wating to run  服务关闭");
            }
        }
    }

    public static Observable<Integer> pushCol(List<Integer> ids, ForkJoinPool forkJoinPool) {
        return Observable.create(p -> {
            AtomicInteger atomicInteger = new AtomicInteger(ids.size());
            forkJoinPool.submit(() -> {
                ids.forEach(id -> {
                    p.onNext(id);
                    if(atomicInteger.decrementAndGet() == 0) {
                        p.onComplete();
                    }
                });
            });
        });
    }
}