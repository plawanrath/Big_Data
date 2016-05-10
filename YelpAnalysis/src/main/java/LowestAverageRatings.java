import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;
import java.util.*;

/**
 * Created by Plawan on 2/13/16.
 */
public class LowestAverageRatings {
    public static class BusinessMap extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        private Text word = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException,
                InterruptedException {
            String dilm = "^";
            String[] dataValue = StringUtils.split(value.toString(), dilm);
            word.set(dataValue[dataValue.length-2]);
            Double rating = Double.parseDouble(dataValue[dataValue.length-1]);
            context.write(word, new DoubleWritable(rating));
        }

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
        }
    }

    /**
     * The reducer retrieves every word and puts it into a Map: if the word already exists in the
     * map, increments its value, otherwise sets it to 1.
     */
    public static class TopNReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

        private Map<Text, DoubleWritable> countMap = new HashMap<Text, DoubleWritable>();

        @Override
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {

            // computes the number of occurrences of a single word
            double sum = 0;
            int count = 0;
            for (DoubleWritable val : values) {                 //The idea here is that, I store Ratings as the values for Map, and use a separate count to get the average rating.
                sum += val.get();
                count++;
//                countMap.put(new Text(key), new DoubleWritable(val.get()));
            }

            double res = sum/count;   //Get and store average rating in HashMap for Cleanup
            countMap.put(new Text(key), new DoubleWritable(res));

        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {

            Map<Text, DoubleWritable> sortedMap = sortByValues(countMap);

            int counter = 0;
            for (Text key : sortedMap.keySet()) {
                if (counter++ == 10) {
                    break;
                }
                context.write(key, sortedMap.get(key));
            }
        }
    }

    /*
   * sorts the map by values. Taken from:
   * http://javarevisited.blogspot.it/2012/12/how-to-sort-hashmap-java-by-key-and-value.html
   */
    private static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        Map<K, V> sortedMap = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: Top10BusinessRating <in> <out>");
            System.exit(2);
        }

        Job job = Job.getInstance(conf, "CountYelp");
        job.setJarByClass(LowestAverageRatings.class);

        job.setMapperClass(BusinessMap.class);
        job.setReducerClass(TopNReducer.class);
        //uncomment the following line to add the Combiner
        //job.setCombinerClass(Reduce.class);

        // set output key type

        job.setOutputKeyClass(Text.class);


        // set output value type
        job.setMapOutputValueClass(DoubleWritable.class);
        job.setOutputValueClass(DoubleWritable.class);


        //set the HDFS path of the input data
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        // set the HDFS path for the output
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

        //Wait till job completion
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
