package io.druid.segment.data;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.google.common.collect.ImmutableList;
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.ImmutableBitmap;
import com.metamx.collections.bitmap.MutableBitmap;
import com.metamx.common.logger.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 *
 */
@RunWith(Parameterized.class)
public class BitmapCreationBenchmark extends AbstractBenchmark
{
  private static final Logger log = new Logger(BitmapCreationBenchmark.class);

  @Parameterized.Parameters
  public static List<Class<? extends BitmapSerdeFactory>[]> factoryClasses()
  {
    return Arrays.<Class<? extends BitmapSerdeFactory>[]>asList(
        (Class<? extends BitmapSerdeFactory>[]) Arrays.<Class<? extends BitmapSerdeFactory>>asList(
            ConciseBitmapSerdeFactory.class
        ).toArray(),
        (Class<? extends BitmapSerdeFactory>[]) Arrays.<Class<? extends BitmapSerdeFactory>>asList(
            RoaringBitmapSerdeFactory.class
        ).toArray()
    );
  }

  final BitmapFactory factory;
  final ObjectStrategy<ImmutableBitmap> objectStrategy;

  public BitmapCreationBenchmark(Class<? extends BitmapSerdeFactory> clazz)
      throws IllegalAccessException, InstantiationException
  {
    BitmapSerdeFactory serdeFactory = clazz.newInstance();
    factory = serdeFactory.getBitmapFactory();
    objectStrategy = serdeFactory.getObjectStrategy();
  }

  private static final int numBits = 100000;


  static Random random;
  static int[] randIndex = new int[numBits];

  @AfterClass
  public static void cleanupAfterClass()
  {
    List<Class<? extends BitmapSerdeFactory>[]> classes = factoryClasses();
    for (int i = 0; i < classes.size(); ++i) {
      log.info("Entry [%d] is %s", i, classes.get(i)[0].getCanonicalName());
    }
  }

  @BeforeClass
  public static void setupBeforeClass()
  {
    for (int i = 0; i < numBits; ++i) {
      randIndex[i] = i;
    }
    // Random seed chosen by hitting keyboard with BOTH hands... multiple times!
    random = new Random(78591378);
    for (int i = 0; i < numBits; ++i) {
      int idex = random.nextInt(randIndex.length);
      int swap = randIndex[i];
      randIndex[i] = randIndex[idex];
      randIndex[idex] = swap;
    }
  }

  ImmutableBitmap baseImmutableBitmap;
  MutableBitmap baseMutableBitmap;
  byte[] baseBytes;

  @Before
  public void setup()
  {
    baseMutableBitmap = factory.makeEmptyMutableBitmap();
    for (int i = 0; i < numBits; ++i) {
      baseMutableBitmap.add(i);
    }
    baseImmutableBitmap = factory.makeImmutableBitmap(baseMutableBitmap);
    baseBytes = baseImmutableBitmap.toBytes();
  }


  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 1000)
  @Test
  public void testLinearAddition()
  {
    MutableBitmap mutableBitmap = factory.makeEmptyMutableBitmap();
    for (int i = 0; i < numBits; ++i) {
      mutableBitmap.add(i);
    }
    Assert.assertEquals(numBits, mutableBitmap.size());
  }

  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 10)
  @Test
  public void testRandomAddition()
  {
    MutableBitmap mutableBitmap = factory.makeEmptyMutableBitmap();
    for (int i : randIndex) {
      mutableBitmap.add(i);
    }
    Assert.assertEquals(numBits, mutableBitmap.size());
  }

  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 1000)
  @Test
  public void testLinearAdditionDescending()
  {
    MutableBitmap mutableBitmap = factory.makeEmptyMutableBitmap();
    for (int i = numBits - 1; i >= 0; --i) {
      mutableBitmap.add(i);
    }
    Assert.assertEquals(numBits, mutableBitmap.size());
  }


  @BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 1000)
  @Test
  public void testToImmutableByteArray()
  {
    ImmutableBitmap immutableBitmap = factory.makeImmutableBitmap(baseMutableBitmap);
    Assert.assertArrayEquals(baseBytes, immutableBitmap.toBytes());
  }

}
