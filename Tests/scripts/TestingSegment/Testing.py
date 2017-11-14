import cv2
import numpy as np
import copy
import matplotlib.pyplot as plt
from os import listdir




def load_images(source,destination):
    # return array of images
    names = listdir(source)
    i = 1

    for img in names:
        location = source
        analyze(location, destination,img)
        left = len(names)-i
        print('Images left to process ',left)
        i += 1


def analyze(path, destination,imgName):
    original = cv2.imread(path + '\\' + imgName, cv2.IMREAD_COLOR )
    img = cv2.cvtColor(original, cv2.COLOR_BGR2GRAY)
    ret, thresh = cv2.threshold(img, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    scale = 0
    im2, contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
    copyOfThresh = thresh.copy()
    accumulateContours = []

    bluredImage = thresh.copy()



    num_of_contours = len(contours)

    while num_of_contours >= 5 and scale<=20:
        contours = []
        scale += 1
        bluredImage = cv2.blur(thresh, (scale, scale))
       # res = cv2.resize(bluredImage, (300,300))
        #cv2.imshow("test", res)
        #cv2.waitKey(0)
        im2, contours, hierarchy = cv2.findContours(bluredImage, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_NONE)
        for x in contours:
            area = cv2.contourArea(x)
            if area > 500:
                accumulateContours.append(x)


        num_of_contours = len(accumulateContours)
       # print('num of contours ',num_of_contours)



    kernel = np.ones((15, 15), np.uint8)
    cv2.erode(bluredImage, kernel, iterations=1)
    cv2.dilate(bluredImage, kernel, iterations=1)
    #res = cv2.resize(bluredImage, (300, 300))
    #cv2.imshow("test", res)
    #cv2.waitKey(0)
    ret, thresh2 = cv2.threshold(bluredImage, 127, 255, cv2.THRESH_BINARY_INV)


    #backToBgr = cv2.cvtColor(res, cv2.COLOR_GRAY2RGB)
    #draw = cv2.drawContours(res, accumulateContours, -1, (0, 0, 255), 2)

    # for x in accumulateContours:
    # print(cv2.contourArea(x))
    _ = cv2.imwrite(destination+'\\'+imgName, thresh2)


load_images('C:\challenge\Training\ISIC-2017_Training_Data', 'C:\challenge\Training\python_output_training')
#analyze('C:\challenge\ISIC-2017_Validation_Data\ISIC-2017_Validation_Data', 'ISIC_0001769.jpg')
