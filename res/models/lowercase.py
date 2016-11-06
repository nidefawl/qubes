import os
rootdir="."
fileList=[]

for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        filepath = subdir + os.sep + file
        os.rename(subdir + os.sep +file, subdir + os.sep +file.lower())
        # break
        # if filepath.endswith(".png"):
            # fileList.append(filepath)
