import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import sys
import codecs
import os
import linecache
import pylab

path = 'out'

args = sys.argv
#引数の処理
if len(args) <= 1:
    print("参照するファイルが見つかりません！")
    exit(0)
if len(args) > 1:
    path = args[1]


root = [f for f in os.listdir(path) if '_FR.csv' in f]

#グラフのスケール画像を生成
def scale_image(label, max):
    fig, ax = plt.subplots(figsize=(5, 5))  # 画像サイズ
    fig.set_figheight(1)  # 高さ調整

    plt.xlim(0, max)
    plt.xlabel(label, fontsize=12)

    ax.set_frame_on(False)
    ax.get_xaxis().tick_bottom()
    ax.axes.get_yaxis().set_visible(False)
    ax.tick_params(direction='inout', length = 10, labelsize=10)

    xmin, xmax = ax.get_xaxis().get_view_interval()
    ymin, ymax = ax.get_yaxis().get_view_interval()
    ax.add_artist(pylab.Line2D((xmin, xmax), (ymin, ymin), color='black', linewidth=2))


    ax.text(max, -0.05, round(max,2), fontsize=10)

    plt.tight_layout()

    #plt.show()
    plt.savefig(path+'/scale_'+label+'.png')

    plt.close()

#全データ中から軸の最大値を発見
def data_max(root):
    n = 0
    r = 0.0

    for f in root:
        file = path+'/'+f
        lines = codecs.open(file, encoding='SJIS').readlines()
        nmax = int(lines[3].split(',')[1])
        rmax = float(lines[len(lines)-1].split(',')[3])

        if n < nmax:
            n = nmax

        if r < rmax:
            r = rmax

    n += int(n * 0.05)
    r += r * 0.05

    return n, r

#データのグラフ化
def graph(file, data, setting_info, analize_info, pmax, rmax, xmax):
    fig = plt.figure()
    fig.subplots_adjust(left=0.05, bottom=0.1, right=0.95, top=0.95, wspace=0.1, hspace=0.1)
    #fig.subplots_adjust(right=0.85)
    #fig.subplots_adjust(left=0.15)

    ax1 = fig.add_subplot(1, 1, 1)
    ax2 = ax1.twinx()
    #ax3 = ax1.twinx()

    ax1.set_xlabel(setting_info.split(',')[1] + '(dx' + setting_info.split(',')[3] + ')', fontsize=12)

    ax1.set_ylabel('P', fontsize=12)
    ax2.set_ylabel('FR', fontsize=12)
    #ax3.set_ylabel('Count')

    #rspine = ax3.spines['right']
    #rspine.set_position(('axes', 1.1))

    plt.text(0.01, 0.9, analize_info.replace(',', '\n'), horizontalalignment='left', verticalalignment='top', family='monospace',
             transform=ax1.transAxes, fontsize=12)
    plt.xlim(0, xmax)
    ax1.set_ylim(0, pmax)
    ax2.set_ylim(0, rmax)

    ax1.plot(data.index, data['COUNT'], label='Population')
    ax2.plot(data.index, data['RATE'], c='red', label='Failure Rate')
    #ax3.bar(data.index, data['FAIL'], label='Failure Count')

    #軸の数値を削除
    ax1.set_xticklabels([])
    ax1.set_yticklabels([])
    ax2.set_yticklabels([])

    #h1, l1 = ax1.get_legend_handles_labels()
    #h2, l2 = ax2.get_legend_handles_labels()
    #h3, l3 = ax3.get_legend_handles_labels()
    #ax1.legend(h1 + h2 + h3, l1 + l2 + l3)
    #ax1.legend(h1 + h2, l1 + l2)

    #plt.xticks(color="None")
    #plt.yticks(color="None")

    #plt.show()

    plt.savefig(file.replace('.csv', '.png'))

    plt.close()

def process_file(pmax, rmax, xmax):
    for f in root:
        file = path+'/'+f
        data = pd.read_csv(file, index_col=0, delimiter=',', header = 2, encoding='SJIS')
        data = data[data.index <= xmax]

        lines = codecs.open(file, encoding='SJIS').readlines()
        setting_info = lines[0].strip()
        analize_info = lines[1].strip()

        #print(setting_info)
        print(f.replace('_FR.csv', '')+':'+analize_info)
        #print(data)

        graph(file, data, setting_info, analize_info, pmax, rmax, xmax)

if __name__ == '__main__':
    n, r = data_max(root)
    print('max N='+str(n)+' max R='+str(r))

    x = 10000

    scale_image('SMR', x)
    scale_image('Population (P)', n)
    scale_image('Failure Rate (FR)', r)

    process_file(n, r, x)