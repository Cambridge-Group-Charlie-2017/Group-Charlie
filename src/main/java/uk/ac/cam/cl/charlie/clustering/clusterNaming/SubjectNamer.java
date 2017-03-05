package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang.WordUtils;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;

/**
 * Given an cluster of emails generates and sets the name for the cluster based
 * on the emails in that clusters contents and sets the given clusters name to
 * that
 *
 * @author M Boyce
 * @author Gary Guo
 */
public class SubjectNamer extends ClusterNamer {

    private static double MIN_PROPORTION_CORRECT = 0.8;

    @Override
    public NamingResult name(Cluster<Message> cluster) {
        ArrayList<ClusterableObject<Message>> messages = cluster.getObjects();

        PositionedWordCounter counter = new PositionedWordCounter();
        // NOTE: wordAveragePositionMap contains sum of position in subject line
        // to start with later
        // divide by its frequency to get aveage position for word ordering

        // TODO: Possibly include body of email in naming
        // TreeMap<String,Integer> wordFrequencyBody = new
        // TreeMap<String,Integer>();

        // Loop through all emails in a cluster and find word frequencies
        for (ClusterableObject<Message> cm : messages) {
            Message m = cm.getObject();

            try {
                counter.count(m.getSubject());
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        // Generate cut off for number of occurrences
        int cutOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        List<String> wordsToUse = counter.words().stream()
                // Remove all stop words
                .filter(w -> !stopWords.contains(w))
                // Sort by descending frequency
                .sorted((w1, w2) -> counter.frequency(w2) - counter.frequency(w1))
                // Take only top 5
                .limit(5)
                // Enforce cut-off
                .filter(w -> counter.frequency(w) > cutOff)
                // Sort in relative position order
                .sorted((w1, w2) -> counter.position(w1) - counter.position(w2))
                // Convert back to list
                .collect(Collectors.toList());

        // Generate cluster name
        String clusterName = String.join(" ", wordsToUse);

        if (!clusterName.isEmpty())
            return new NamingResult(WordUtils.capitalize(clusterName), 1);

        return null;
    }

}
