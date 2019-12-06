import pandas as pd
import matplotlib.pyplot as plt
import sys
import codecs
from scipy import stats

path = 'out'
files = []

args = sys.argv
#引数の処理
if len(args) <= 1:
    print("データが選択されていません")
    exit(0)
if len(args) > 3:
    path = args[1]
    for i in range(2,len(args)):
        files.append(args[i].replace('_', ',')+'_FR.csv')


def kstest(frdata, ax):
    keys = list(frdata.keys())

    r = []
    v = []

    if len(keys) > 1:
        for i in range(0,len(keys)-1):
            for j in range(i, len(keys)):
                if i==j:
                    continue
                n = keys[i]+' VS '+keys[j]
                p = stats.ks_2samp(frdata[keys[i]], frdata[keys[j]]).pvalue

                s = ""
                if p < 0.01:
                    s = "{0:.3f} **".format(p)
                else :
                    if p < 0.05:
                        s = "{0:.3f} *".format(p)
                    else:
                        s = "{0:.3f} *".format(p)
                r.append(n)
                v.append(s)
        ax.table(cellText=[r, v], rowLabels=['Compare', 'p'],loc="center", cellLoc='center')
    else:
        ax.table(cellText=[['None'], ['None']], rowLabels=['Compare', 'p'], loc="center", cellLoc='center')

#データのグラフ化
def graph(info, data, ax1, ax2):
    ax1.plot(data.index, data['COUNT'], ls='--', label=info)
    ax2.plot(data.index, data['RATE'], label=info)

    #plt.text(0.01, 0.9, analize_info.replace(',', '\n'), horizontalalignment='left', verticalalignment='top', family='monospace',
    #         transform=ax1.transAxes, fontsize=12)

def process_file(fs, xmax):
    fig = plt.figure()
    fig.subplots_adjust(left=0.05, bottom=0.1, right=0.95, top=0.95, wspace=0.1, hspace=0.1)

    ax = fig.add_axes((0.1, 0.3, 0.75, 0.65))
    ax3 = fig.add_axes((0.15, 0.0, 0.75, 0.2))
    ax2 = ax.twinx()
    ax3.axis('off')

    plt.xlim(0, xmax)
    info = ''

    frdata = {}

    for f in fs:
        file = path+'/'+f
        data = pd.read_csv(file, index_col=0, delimiter=',', header = 2, encoding='SJIS')

        lines = codecs.open(file, encoding='SJIS').readlines()
        setting_info = lines[0].strip()
        analize_info = lines[1].strip()

        label = f.replace('.csv', '')+'  '+analize_info
        print(label)


        frdata[f.replace('_FR.csv', '')] = data['RATE']

        info = setting_info

        graph(label, data, ax, ax2)

    ax.set_ylabel('Population', fontsize=12)
    ax2.set_ylabel('Failure Rate', fontsize=12)
    ax.set_xlabel(info.split(',')[1] + '(dx' + info.split(',')[3] + ')', fontsize=12)

    #KS検定
    kstest(frdata, ax3)

    plt.legend()
    #plt.show()
    plt.savefig(path+'/compare_score.png')


if __name__ == '__main__':
    x = 10000
    process_file(files, x)