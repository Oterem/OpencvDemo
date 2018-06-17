from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import boto3
import subprocess
import numpy as np
import tensorflow as tf
# import keras

import argparse
import os.path
import re
import sys
import urllib
import json

def downloadFromS3(strBucket,strKey,strFile):
    s3_client = boto3.client('s3')
    s3_client.download_file(strBucket, strKey, strFile)

def create_graph():
    with tf.gfile.FastGFile(os.path.join('/tmp/imagenet/', 'classify_image_graph_def.pb'), 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        _ = tf.import_graph_def(graph_def, name='')

def run_inference_on_image(image):
    if not tf.gfile.Exists(image):
        tf.logging.fatal('File does not exist %s', image)
    image_data = tf.gfile.FastGFile(image, 'rb').read()

  # Creates graph from saved GraphDef.
    create_graph()
    label_lines = ['melanoma', 'non_melanoma']

    with tf.Session() as sess:
        # Some useful tensors:
        # 'softmax:0': A tensor containing the normalized prediction across
        #   1000 labels.
        # 'pool_3:0': A tensor containing the next-to-last layer containing 2048
        #   float description of the image.
        # 'DecodeJpeg/contents:0': A tensor containing a string providing JPEG
        #   encoding of the image.
        # Runs the softmax tensor by feeding the image_data as input to the graph.
        softmax_tensor = sess.graph.get_tensor_by_name('final_result:0')
        predictions = sess.run(softmax_tensor,
                               {'DecodeJpeg/contents:0': image_data})
        
       

        # Creates node ID --> English string lookup.
        #node_lookup = NodeLookup()
        
        top_k = predictions[0].argsort()[-len(predictions[0]):][::-1]
        #strResult = '%s (score = %.5f)' % (node_lookup.id_to_string(top_k[0]), predictions[top_k[0]])
        data = {}
        
        for node_id in top_k:
            human_string = label_lines[node_id]
            score = predictions[0][node_id]
            data[human_string] = score
            #str = '%s (score = %.5f)' % (human_string, score)
            #print('%s (score = %.5f)' % (human_string, score))
        return data
        
  
def upload_data_to_s3(data):
    res = json.dumps(data, indent=4, sort_keys=True, default=str)
    s3_client = boto3.client('s3')
    file_key = (data["imageName"]+data["uploadedTime"]).replace('.', '_') + ".json"
    print(file_key)
    s3_client.put_object(ACL='public-read-write', Body=res , Bucket= 'moleagnose-results', Key=file_key)

    

def handler(event, context):
    print(event)
    print(context)
    if not os.path.exists('/tmp/imagenet/'):
        os.makedirs('/tmp/imagenet/')
	upload_bucket = 'moleagnose-images'
	download_bucket = 'moleagnose-results'

    strBucket = 'moleagnose-deployment'
    strKey = 'retrained_labels.txt'
    strFile = '/tmp/imagenet/imagenet_synset_to_human_label_map.txt'
    downloadFromS3(strBucket,strKey,strFile)  
    print(strFile)

    strBucket = 'moleagnose-deployment'
    strKey = 'retrained_graph.pb'
    strFile = '/tmp/imagenet/classify_image_graph_def.pb'
    downloadFromS3(strBucket,strKey,strFile)
    print(strFile)

    image_name = event["Records"][0]["s3"]["object"]["key"]
    image_path = 'https://s3.amazonaws.com/moleagnose-images/'+ image_name
    print(image_path)
    strFile = '/tmp/imagenet/inputimage.jpg'
    downloadFromS3('moleagnose-images',image_name,strFile)
    #urllib.urlretrieve(image_path, strFile)

    image = os.path.join('/tmp/imagenet/', 'inputimage.jpg')
    data = run_inference_on_image(image)
    data["uploadedTime"] = event["Records"][0]["eventTime"]
    data["imageName"] = event["Records"][0]["s3"]["object"]["key"]
    data["imageUrl"] = image_path
    mel_val = data["melanoma"]
    non_mel_val = data["non_melanoma"]
    if mel_val > non_mel_val:
         data["bigger"] = {"name":"melanoma", "value":mel_val}
    else:
        data["bigger"] = {"name":"non_melanoma", "value":non_mel_val}
        
    data["diff"] = abs(non_mel_val-mel_val)
    upload_data_to_s3(data)
    
    
   
 