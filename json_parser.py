#! /usr/bin/python
import json
import sys
from pprint import pprint
from os.path import basename
import codecs
import gzip


fil=sys.argv[1]
with codecs.open(fil, 'r', 'utf-8') as f:
    lines = f.readlines()

json_file=("%s_edited.json") % (fil)
f1 = codecs.open(json_file,'w', 'utf-8')
for line in lines:
#    print line
    if line.strip().startswith('{') == True and line.strip()[-1] == '}':
#        print line
        f1.write(line) # python will convert \n to os.linesep

f1.close()        

print "Json format filtered"

tweet_file_trec_format=("%s_trec_format.txt") % (fil)

f1 = codecs.open(tweet_file_trec_format,'w', 'utf-8')
with codecs.open(json_file, 'r', 'utf-8') as f:
    lines = f.readlines()

docid_prefix=basename(fil).replace("statuses.log.","")
i=0

for line in lines:
    try:
        data = json.loads(line)
        json.dumps(data, sort_keys=True, indent=4)

        while 1:
            userid=data['user']['id']
            tweetid, tweettext=data['id'], data['text'].strip().replace('\n',' ').replace('\r',' ')
            tweettime=data['created_at']
            follower=data['user']['followers_count']
            friend= data['user']['friends_count']
            listed=data['user']['listed_count']
            status, retweet=data['user']['statuses_count'], data['retweet_count']
            favourite=data['favorite_count']
            lang=data['lang']
            docid=("%s-%0.12d") % (docid_prefix,i)

            IfitisRetweeted=data['retweeted_status']
            if IfitisRetweeted:
                #print IfitisRetweeted
                retweeted_from=IfitisRetweeted['id']
                output=("<DOC>\n<DOCNO>%s</DOCNO>\n<tweettime>%s</tweettime>\n<time>%s</time>\n<userid>%s</userid>\n<follower>%s</follower>\n<friend>%s</friend>\n<listed>%s</listed>\n<status>%s</status>\n<retweet>%s</retweet>\n<favourite>%s</favourite>\n<lang>%s</lang>\n<retweetedfrom>%s</retweetedfrom>\n<TEXT>%s</TEXT>\n</DOC>\n\n") % (docid,tweetid,tweettime,userid,follower,friend,listed,status,retweet,favourite,lang,retweeted_from,tweettext)
                f1.write(output)
            i=i+1
            data=IfitisRetweeted
        
        

    except (ValueError, KeyError, TypeError) as e:
        #print str(e)
        if str(e) == '\'retweeted_status\'':
#            print str(e)
            output=("<DOC>\n<DOCNO>%s</DOCNO>\n<tweettime>%s</tweettime>\n<time>%s</time>\n<userid>%s</userid>\n<follower>%s</follower>\n<friend>%s</friend>\n<listed>%s</listed>\n<status>%s</status>\n<retweet>%s</retweet>\n<favourite>%s</favourite>\n<lang>%s</lang>\n<retweetedfrom>null</retweetedfrom>\n<TEXT>%s</TEXT>\n</DOC>\n\n") % (docid,tweetid,tweettime,userid,follower,friend,listed,status,retweet,favourite,lang,tweettext)
            f1.write(output)
            i=i+1
        else:
            continue
f1.close()
print "Converted to TREC format"
