/**
 * Created by plawanrath on 2/4/16.
 */
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.List;

public class TestTwitter {

    public static void main(String[] args) {

        try {
            Twitter4jProperties t4j = new Twitter4jProperties();
            String consumerKey=t4j.getConsumerKey();
            String consumerSecret=t4j.getConsumerSecret();
            String accessToken=t4j.getAccessToken();
            String accessTokenSecret=t4j.getAccessTokenSecret();
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret)
                    .setOAuthAccessToken(accessToken)
                    .setOAuthAccessTokenSecret(accessTokenSecret);

            TwitterFactory tf = new TwitterFactory(cb.build());
            // gets Twitter instance with default credentials
            Twitter twitter = tf.getInstance();
            User user = twitter.verifyCredentials();
            List<Status> statuses = twitter.getHomeTimeline();
            System.out.println("Showing @" + user.getScreenName() + "'s home timeline.");
            for (Status status : statuses) {
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
            }
            List<Status> OtherUserStatus = twitter.getUserTimeline("CameronNewton");
            System.out.println("Showing @CameronNewton's timeline");
            for(Status tweet : OtherUserStatus) {
                System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
            }
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }
    }
}
