# -*- coding: utf-8 -*-

# parse training

import json
import re
import datetime
import codecs

from nltk.corpus import brown

### bigramProb.py ###
# - This file is used simply as a tool to process the text from the Brown Corpus and Stanford Question Answering Dataset.
# - This is the only python file, it does not interact with the rest of the proram.
# - Text from the datasets are processed into bigrams. Each bigram is calculated a probability for its appearance within the test data.
# - Bigrams and their probabilities are exported to validator_bigram_prob.txt in the format:  word1/word2=probability
#     as this is how the validator component in the rest of the program will read in the values.

## Create bigrams from the brown corpus
def import_brown():

    b_count = 0

    total_tokens = 0

    unigram_count_dict = {}
    bigram_count_dict = {}

    categories = ["adventure", "science_fiction", "mystery", "fiction", "lore", "hobbies"]
    #categories = ["mystery"]

    for category in categories:
        print category, datetime.time()
        total_tokens += len(brown.words(categories=category))
        current_tokens = [word.lower() if word != "." else "%" for word in brown.words(categories=category)]
        current_tokens = [word for word in current_tokens if word != "," and word !="''" \
                          and word != "``" and word !=":" and word !=";"]

        current_tokens.insert(0, "%")
            
        print "uni/bi"
        for i in range(len(current_tokens)-1):

            if current_tokens[i] in unigram_count_dict.keys():
                unigram_count_dict[current_tokens[i]] += 1
            else:
                unigram_count_dict[current_tokens[i]] = 1
                #print unigram_count_dict[token] 

            if current_tokens[i] != "?" and current_tokens[i+1] != "?":
                curr_bigram = current_tokens[i]+"/"+current_tokens[i+1]
                b_count += 1
            #print curr_bigram

            if b_count % 5000 == 0:
                print b_count

        
            if curr_bigram in bigram_count_dict.keys():
                    bigram_count_dict[curr_bigram] += 1
            else:
                bigram_count_dict[curr_bigram] = 1

            if current_tokens[len(current_tokens)-1] in unigram_count_dict.keys():
                unigram_count_dict[current_tokens[len(current_tokens)-1]] += 1
            else:
                unigram_count_dict[len(current_tokens)-1] = 1

                
    print len(bigram_count_dict.keys())

    return bigram_count_dict, unigram_count_dict



## Create bigrams from the Stanford Question Answering Dataset and write to file.
def import_json(bigram_count_dict, unigram_count_dict):

    print "entered import_json"

    with open("train-v1.1.json", 'r') as f:
        
        train = json.load(f)

    print "json loaded"

    #bigram_count_dict = {}
    #unigram_count_dict = {}

    bigram_prob_dict = {}

    print len(train['data'])

    for i in range(len(train['data'])):
        if i % 5 == 0:
            print i
    #for i in range(0, 10):
        for j in range(len(train['data'][i]['paragraphs'])):

            try:
                current_question = train['data'][i]['paragraphs'][j]['qas'][1]['question']
                current_question = re.sub(r'\?', r' ?', current_question)
                current_question = re.sub(r'!', r' !', current_question)
                current_question = re.sub(r',', r'', current_question)
                current_question = re.sub(r'\.', r' .', current_question)
                current_question = re.sub(r'\'', r' ', current_question)
                current_question = re.sub(r'"', r'', current_question)

                tokens = ["%"]
                

                for token in re.split(" ", current_question):
                    if token != "," and token != "s" and token != " ":
                        tokens.append(token.lower())

                for token in tokens:
                    #print token
                    
                    if token in unigram_count_dict.keys():
                        unigram_count_dict[token] += 1
                        #print unigram_count_dict[token]
                    else:
                        unigram_count_dict[token] = 1
                        #print unigram_count_dict[token] 


                for i in range(len(tokens)-1):
                    curr_bigram = tokens[i]+"/"+tokens[i+1]
                    #print curr_bigram

                    if curr_bigram in bigram_count_dict.keys():
                        bigram_count_dict[curr_bigram] += 1
                    else:
                        bigram_count_dict[curr_bigram] = 1
                
                #print train['data'][i]['paragraphs'][j]['qas'][1]['question']

            except:
                pass

   
    print "num of sections", len(train['data'])

    total_tokens = len(unigram_count_dict.keys())
    print "total tokens:", total_tokens
    total_bigrams = len(bigram_count_dict.keys())
    print "total bigrams:", total_bigrams

    
    #for key in unigram_count_dict.keys():
    #    print key, "-", unigram_count_dict[key]

    once = 0
    more = 0

    for key in sorted(bigram_count_dict.keys()):

        word1 = re.split("/", key)[0]

        try:
            bigram_prob_dict[key] = float(bigram_count_dict[key]) / unigram_count_dict[word1]
            if bigram_prob_dict[key] != 1.0:
            #    print key, "-", bigram_prob_dict[key]
                more += 1
            else:
                once += 1
        except:
            pass

        #print key, "-", bigram_count_dict[key]
        
    print "once", once, "more", more
    
    ## Write bigrams to file
    bi_prob = codecs.open("validator_bigram_prob.txt", "w", "utf-8")
    for key in bigram_prob_dict.keys():
        try:
            bi_prob.write(key+"="+unicode(str(bigram_prob_dict[key])+"\n", "utf-8"))
        except:
            pass

    bi_prob.close()

    ## Write unigrams to file
    uni_prob = codecs.open("validator_unigram_prob.txt", "w", "utf-8")
    for key in unigram_count_dict.keys():
        try:
            uni_prob.write(key+"="+unicode(str(float(unigram_count_dict[key])/total_tokens)+"\n", "utf-8"))
        except:
            pass

    uni_prob.close()

    #with open("validator_bigram_prob.txt", "w") as v:
    #    for key in bigram_prob_dict.keys():
    #        v.write(key+"="+str(bigram_prob_dict[key])+"\n")


    # unigram denominator should be total tokens not total types

    #with open("validator_unigram_prob.txt", "w") as u:
    #    for key in unigram_count_dict.keys():
    #        u.write(key+"="+str((float(unigram_count_dict[key])/total_tokens)+"\n"))

    return bigram_prob_dict, bigram_count_dict, unigram_count_dict

## Run to generate bigrams/unigrams if neccessary. However, this takes a long time as the datasets are quite large.
##   Just use the already prepared files in Reading-Comprehension_Question-Generator/BigramModel/ :)
def main():


    bigram_count_dict, unigram_count_dict = import_brown()
    print "brown complete"
    
    #bigram_count_dict = {}
    #unigram_count_dict = {}
    
    import_json(bigram_count_dict, unigram_count_dict)



if __name__ == "__main__":
    main()

