import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import sys

file = 'out/mainte_score.csv'

g = 'SCORE'
xs = 'AGE'
ys = 'AVG'

args = sys.argv
#引数の処理

if len(args) <= 1:
    print("参照するファイルが見つかりません！")
    exit(0)
if len(args) > 1:
    file = args[1]
if len(args) > 2:
    xs = args[2]
if len(args) > 3:
    ys = args[3]
if len(args) > 4:
    g = args[4]

data = pd.read_csv(file, index_col='SID', delimiter=',', encoding='SJIS')

#print(data)
n = int(max(data[g]))

#clusters = data.iloc[:,len(data.columns)-1].values
#axdata = data.iloc[:,[1,len(data.columns)-2]]
#print([clusters.tolist()])

cl =data[g].tolist()

for i in range(0,n+1):
    c = data[data[g] == i]
    #print(c)
    name = 'C'+str(i)
    plt.scatter(c[xs], c[ys], c = cm.cmaps_listed.get(cl[i]), s=5,  label=name)

#print('cluster:'+str(len(set(data[g].values))))

plt.xlabel(xs)
plt.ylabel(ys)

#plt.show()

plt.savefig(file.replace('.csv', '.png'))
plt.close()