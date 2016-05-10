import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Progressable;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.lang.Thread;

/**
 * Created by plawanrath on 2/4/16.
 */
public class TweetDataGenerator {

    public static void FileCopyWithToHFDSProgress(String src, String dest) throws Exception
    {

        InputStream in = new BufferedInputStream(new FileInputStream(src));

        Configuration conf = new Configuration();
        conf.addResource(new Path("/usr/local/hadoop-2.4.1/etc/hadoop/core-site.xml"));
        conf.addResource(new Path("/usr/local/hadoop-2.4.1/etc/hadoop/hdfs-site.xml"));

        FileSystem fs = FileSystem.get(URI.create(dest), conf);
        OutputStream out = fs.create(new Path(dest), new Progressable() {
            public void progress() {
                System.out.print(".");
            }
        });

        IOUtils.copyBytes(in, out, 4096, true);
    }

    public static void main(String[] args) throws Exception {

        if(args.length < 3)
        {
            System.err.println("There have to be 5 arguments. <search term> <Output File-path> <HDFS-Path>");
            System.exit(-1);
        }
        final int numberOfTweets = 500;
        HashMap<String, String> FromAndTo = new HashMap<String, String>()
        {{
            put("2016-02-03", "2016-02-04");
            put("2016-02-04", "2016-02-05");
            put("2016-02-05", "2016-02-06");
            put("2016-02-06", "2016-02-07");
            put("2016-02-07", "2016-02-08");
            put("2016-02-08", "2016-02-09");
        }};
        String search_term = args[1];//"NFL";
/*
String startingFrom = args[2];//"2016-02-03";
String until = args[3];//"2016-02-04";
int numberOfTweets = Integer.parseInt(args[4]); //1000;
*/
        String outputFilePath = args[2]; //tmp Output path
        String HDFSdst = args[3]; //HDFS destination

        for (Entry<String, String> pair : FromAndTo.entrySet())
        {
//        Iterator it = FromAndTo.entrySet().iterator();
//        while(it.hasNext()) {
//            Entry<String, String> pair = (Entry) it.next();
            String startingFrom = pair.getKey();
            String until = pair.getValue();
            String outputFile = outputFilePath + "/" + search_term + until + ".txt";
            PrintStream out = new PrintStream(new FileOutputStream(outputFile));
            System.setOut(out);


            Twitter4jProperties t4j = new Twitter4jProperties();
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true).setOAuthConsumerKey(t4j.getConsumerKey())
                    .setOAuthConsumerSecret(t4j.getConsumerSecret())
                    .setOAuthAccessToken(t4j.getAccessToken())
                    .setOAuthAccessTokenSecret(t4j.getAccessTokenSecret());

            Twitter twitter = new TwitterFactory(cb.build()).getInstance();
            Query query = new Query(search_term);
            query.setLang("en");
            query.setSince(startingFrom);
            query.setUntil(until);

            long lastID = Long.MAX_VALUE;
            ArrayList<Status> tweets = new ArrayList<Status>();
            while (tweets.size() < numberOfTweets) {
                if (numberOfTweets - tweets.size() > 100)
                    query.setCount(100);
                else
                    query.setCount(numberOfTweets - tweets.size());
                QueryResult result = twitter.search(query);
                tweets.addAll(result.getTweets());
                for (Status t : tweets)
                    if (t.getId() < lastID) lastID = t.getId();
                query.setMaxId(lastID - 1);
            }

            for (Status tweet : tweets) {
                System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText() + "at time" + tweet.getCreatedAt());
            }
            String UpdatedHDFSdst = HDFSdst + "/" + search_term + until + ".txt";
            TweetDataGenerator.FileCopyWithToHFDSProgress(outputFile, UpdatedHDFSdst);
            String destinationForWCFileInHDFS = HDFSdst + "/" + "Trends" + until;
            String[] argsForWC = {UpdatedHDFSdst, destinationForWCFileInHDFS};
            WordCount2.mainRun(argsForWC);
            Thread.sleep(1000);
        }
    }
}
