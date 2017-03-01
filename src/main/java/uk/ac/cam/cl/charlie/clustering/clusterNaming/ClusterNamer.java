package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;

/**
 * @author M Boyce
 */
public abstract class ClusterNamer {

    public static class NamingResult {
        String name;
        double confidence;

        public NamingResult(String name, double confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }
    }

    public abstract NamingResult name(Cluster<Message> cluster);

    protected static HashSet<String> stopWords = new HashSet<>(StopWords.getStopWords());

    protected static double SHORT_CIRCUIT_CONFIDENCE = 0.8;

    private static List<ClusterNamer> namers = new ArrayList<>();
    static {
        namers.add(new SubjectNamer());
        namers.add(new SenderNamer());
        namers.add(new Word2VecNamer());
        // namers.add(new TfidfNamer());
    }

    /**
     * Generic naming of the cluster that tries all methods and return best
     * matching naming
     *
     * @param cluster
     */
    public static String doName(Cluster<Message> cluster) {
        NamingResult ret = null;
        for (ClusterNamer namer : namers) {
            // Try the namer
            NamingResult result = namer.name(cluster);
            if (result == null)
                continue;

            // If the result is good enough, stop trying other namers
            if (result.confidence > SHORT_CIRCUIT_CONFIDENCE) {
                ret = result;
                break;
            }

            // Replace ret if we are more confident
            if (ret == null) {
                ret = result;
            } else if (result.confidence > ret.confidence) {
                ret = result;
            }
        }

        if (ret == null) {
            String subject;
            try {
                subject = cluster.getObjects().get(0).getObject().getSubject();
            } catch (MessagingException e) {
                subject = "";
            }
            if (subject.equals(""))
                cluster.setName("Failed to name: " + Math.random());
            else
                cluster.setName(subject);
            return null;
        } else {
            cluster.setName(ret.name);
        }

        return cluster.getName();
    }

}
