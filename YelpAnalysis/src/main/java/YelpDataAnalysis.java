import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created by Plawan on 2/13/16.
 */
public class YelpDataAnalysis {

    public static class BusinessMap extends Mapper<LongWritable, Text, Text, IntWritable> {
        private Text word = new Text();
        private final static IntWritable one = new IntWritable(1);

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException,
                InterruptedException {
            String dilm = "^";
            String[] dataValue = StringUtils.split(value.toString(), dilm);

            if (dataValue.length == 3) {
                if (dataValue[1].contains("Palo Alto"))
                    word.set(dataValue[1]);
            }
            context.write(word, one);
        }

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
        }
    }

    public static class Reduce extends Reducer<Text,IntWritable,Text,IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values,Context context ) throws IOException, InterruptedException {

            int count=0;
            for(IntWritable t : values){
                count++;
            }
            context.write(key,new IntWritable(count));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();		// get all args
        if (otherArgs.length != 2) {
            System.err.println("Usage: CountYelpBusiness <in> <out>");
            System.exit(2);
        }

        Job job = Job.getInstance(conf, "CountYelp");
        job.setJarByClass(YelpDataAnalysis.class);

        job.setMapperClass(BusinessMap.class);
        job.setReducerClass(Reduce.class);
        //uncomment the following line to add the Combiner
        //job.setCombinerClass(Reduce.class);

        // set output key type

        job.setOutputKeyClass(Text.class);


        // set output value type
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);


        //set the HDFS path of the input data
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        // set the HDFS path for the output
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

        //Wait till job completion
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
