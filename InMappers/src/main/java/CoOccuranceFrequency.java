import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Co-occurance frequency generator
 * Taken from http://codingjunkie.net/cooccurrence/
 * Created by Plawan on 2/16/16.
 */
public class CoOccuranceFrequency extends Configured implements Tool {

    public static class StripesOccurrenceMapper extends Mapper<LongWritable,Text,Text,MapWritable> {
        private MapWritable occurrenceMap = new MapWritable();
        private Text word = new Text();
        private Set<String> patternsToSkip = new HashSet<String>(Arrays.asList("Hi", "Hello", "Test", "Value", "is", "the"));
        //Since we always skip stop_words, no need to take command line arguments
//        private String input;
        private int wordLength = Integer.MAX_VALUE; //Setting wordLength to max by default which means if length is not specified then all words will be added

        protected void setup(Mapper.Context context)
                throws IOException,
                InterruptedException {
//            if (context.getInputSplit() instanceof FileSplit) {
//                this.input = ((FileSplit) context.getInputSplit()).getPath().toString();
//            } else {
//                this.input = context.getInputSplit().toString();
//            }
            Configuration config = context.getConfiguration();
            if(config.getBoolean("wordcount.length",false)) {
                wordLength = Integer.parseInt(config.get("wordlength"));
            }
        }

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            int neighbors = context.getConfiguration().getInt("neighbors", 2);
            String[] tokens = value.toString().split("\\s+");
            if (tokens.length > 1) {
                for (int i = 0; i < tokens.length; i++) {
                    if(patternsToSkip.contains(tokens[i]))
                        continue;
                    word.set(tokens[i]);
                    occurrenceMap.clear();

                    int start = (i - neighbors < 0) ? 0 : i - neighbors;
                    int end = (i + neighbors >= tokens.length) ? tokens.length - 1 : i + neighbors;
                    for (int j = start; j <= end; j++) {
                        if (j == i) continue;
                        Text neighbor = new Text(tokens[j]);
                        if(occurrenceMap.containsKey(neighbor)){
                            IntWritable count = (IntWritable)occurrenceMap.get(neighbor);
                            count.set(count.get()+1);
                        }else{
                            if(neighbor.toString().length() > wordLength) //checking length
                                continue;
                            occurrenceMap.put(neighbor,new IntWritable(1));
                        }
                    }
                    context.write(word,occurrenceMap);
                }
            }
        }
    }

    public static class StripesReducer extends Reducer<Text, MapWritable, Text, MapWritable> {
        private MapWritable incrementingMap = new MapWritable();

        public void reduce(Text key, Iterable<MapWritable> values, Context context) throws IOException, InterruptedException {
            incrementingMap.clear();
            for (MapWritable value : values) {
                addAll(value);
            }
            context.write(key, incrementingMap);
        }

        private void addAll(MapWritable mapWritable) {
            Set<Writable> keys = mapWritable.keySet();
            for (Writable key : keys) {
                IntWritable fromCount = (IntWritable) mapWritable.get(key);
                if (incrementingMap.containsKey(key)) {
                    IntWritable count = (IntWritable) incrementingMap.get(key);
                    count.set(count.get() + fromCount.get());
                } else {
                    incrementingMap.put(key, fromCount);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new CoOccuranceFrequency(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(getConf(), "wordcount");
        for (int i = 0; i < args.length; i += 1) {
            if("-length".equals((args[i]))) {
                job.getConfiguration().setBoolean("wordcount.length", true);
                i++;
                job.getConfiguration().set("wordlength",args[i]);
            }
        }
        job.setJarByClass(this.getClass());
        // Use TextInputFormat, the default unless job.setInputFormatClass is used
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setMapperClass(StripesOccurrenceMapper.class);
        job.setReducerClass(StripesReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(MapWritable.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

}
