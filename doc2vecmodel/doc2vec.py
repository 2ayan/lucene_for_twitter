import sys
import gensim

# print sys.argv[1], sys.argv[2]
"""
sentences = gensim.models.doc2vec.LabeledLineSentence('/tmp/sentence.tmp')
model = gensim.models.doc2vec.Doc2Vec(sentences, size=200, window=5, min_count=1, workers=4)
model.save_word2vec_format('/tmp/sentenceVectors.txt')
"""

sentences = gensim.models.doc2vec.LabeledLineSentence(sys.argv[1])
model = gensim.models.doc2vec.Doc2Vec(sentences, size=200, window=5, min_count=1, workers=4)
model.save_word2vec_format(sys.argv[2])

"""
# store the model to mmap-able files
model.save('/tmp/my_model.doc2vec')
 
# load the model back
model_loaded = Doc2Vec.load('/tmp/my_model.doc2vec')
"""
