/**
 * wordCount in_Mapper
 * Taken from http://codingjunkie.net/text-processing-with-mapreduce-part1/
 * Created by Plawan on 2/16/16.
 */
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCountInMapper {

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable>{

        private Map<String, Integer> tokenMap;

        protected void setup(Context context) throws IOException,InterruptedException {
            tokenMap = new HashMap<String, Integer>();
        }

        protected void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String token = itr.nextToken();
                Integer count = tokenMap.get(token);
                if(count == null)
                    count = 0;
                count += 1;
                tokenMap.put(token,count);
            }
        }

        protected void cleanup(Context context) throws IOException, InterruptedException {
            IntWritable writableCount = new IntWritable();
            Text text = new Text();
            Set<String> keys = tokenMap.keySet();
            for(String s : keys) {
                text.set(s);
                writableCount.set(tokenMap.get(s));
                context.write(text,writableCount);
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();

        protected void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void run(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        Configuration conf = new Configuration();
        conf.set("mapred.job.tracker", "hdfs://cshadoop1:61120");
        conf.set("yarn.resourcemanager.address", "cshadoop1.utdallas.edu:8032");
        conf.set("mapreduce.framework.name", "yarn");
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCountInMapper.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.waitForCompletion(true);
        long end = System.currentTimeMillis();
        System.out.println("Time taken to run on " + args[0] + " file is " + (end-start) + "ms");
    }

    public static void main(String[] args) throws Exception {
        //Take one argument, the source hdfs path to take all files
        ArrayList<String> files = new ArrayList<String>(Arrays.asList("ArtOfWar","DevilsDictionary"
                ,"Encyclopedia","NotebookOfLDV","OutlineOfScience","SherlockHolmes"));
        String[] arguments = new String[2];
        String path = args[0];
        for (String value : files) {
            arguments[0] = path + "/" + value + ".txt";
            arguments[1] = path + "/" + value + "WCIn";
            WordCountInMapper.run(arguments);
        }
    }

}
