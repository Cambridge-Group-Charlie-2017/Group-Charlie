#!/bin/bash
if [ ! -f src/main/res/word2vec/wordvectors.bin ]; then
    echo "word2vec not found; downloading now"; mkdir -p src/main/res/word2vec; curl -L -o src/main/res/word2vec/wordvectors.bin \
                            https://www.dropbox.com/s/xcbpop8dfblipyp/wordvectors.bin?dl=1
fi
