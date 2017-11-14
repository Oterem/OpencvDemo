
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import PIL.ImageGrab

from PIL import Image

from os import listdir

import numpy as np
import cv2


def start(source, destination):
    names = listdir(source)
    i=1
    length = len(names)
    for img in names:
        location = source
        original = Image.open(source+'\\'+img)
        resize = original.resize((500, 500), Image.ANTIALIAS)
        path_to_save = destination+'\\'+img
        resize.save(path_to_save)
        print(length-i,' left')
        i+=1


start(r'C:\challenge\Validation\ISIC-2017_Validation_Part1_GroundTruth\ISIC-2017_Validation_Part1_GroundTruth',r'C:\challenge\Superpixel\ground_truth')