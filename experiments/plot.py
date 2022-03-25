import matplotlib.pyplot as plt
import numpy as np
import csv

def load(path):
    with open(path, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        # headers = next(reader)
        return np.array(list(reader)).astype(float)

def plot(what, xlabel, ylabel, title):
    vc = load(f'csv/{what}/VC.csv')
    evc = load(f'csv/{what}/EVC.csv')
    revc = load(f'csv/{what}/REVC.csv')
    dmtrevc = load(f'csv/{what}/DMTREVC.csv')

    trim_to = min(len(vc), len(evc), len(revc), len(dmtrevc))
    vc = vc[:trim_to]
    evc = evc[:trim_to]
    revc = revc[:trim_to]
    dmtrevc = dmtrevc[:trim_to]

    x = vc[:, 0]

    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.plot(x, vc[:, 1], label='VC')
    plt.plot(x, evc[:, 1], label='EVC')
    plt.plot(x, revc[:, 1], label='REVC')
    plt.plot(x, dmtrevc[:, 1], label='DMTREVC')
    plt.legend()
    plt.title(title)
    plt.savefig(f"plots/{what}.svg", format="svg")
    plt.clf()
    plt.cla()


plot('time', 'Number of processes', 'Time (s)', "Clock's execution time vs number of processes")
plot('bitsizes', 'Number of events', 'Number of bits', "Growth in the size of the clocks")
