#!/bin/bash
if [ ! -f src/main/resources/word2vec/wordvectors.txt ]; then
    echo "word2vec not found; downloading now"; mkdir -p src/main/resources/word2vec; curl -L -o src/main/resources/word2vec/wordvectors.txt \
                            https://www.dropbox.com/s/8q626nhgrx6ez5m/wordvectors.txt?dl=1
fi
