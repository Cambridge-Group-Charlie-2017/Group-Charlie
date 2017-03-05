package uk.ac.cam.cl.charlie.vec.tfidf;

import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

import javax.mail.Message;
import java.util.HashMap;

/**
 * Created by M Boyce on 23/02/2017.
 */
public class TfidfCachingVectoriser extends TfidfVectoriser {
    private static TfidfCachingVectoriser singleton;
    private HashMap<Message, Vector> vecCache = new HashMap<>();

    protected TfidfCachingVectoriser() {
        super();
    }

    public static TfidfCachingVectoriser getVectoriser() {
        if (singleton == null)
            singleton = new TfidfCachingVectoriser();
        return singleton;
    }
    @Override
    public Vector doc2vec(Message msg) {
        Vector vec = vecCache.get(msg);
        if (vec != null)
            return vec;
        vec = super.doc2vec(msg);
        vecCache.put(msg, vec);
        return vec;
    }
}
