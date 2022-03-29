import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt


def plot(what, x, y, title):
    colnames = ['run', x, y]

    vc = pd.read_csv(f'csv/{what}/VC.csv', header=None, names=colnames)
    evc = pd.read_csv(f'csv/{what}/EVC.csv', header=None, names=colnames)
    revc = pd.read_csv(f'csv/{what}/REVC.csv', header=None, names=colnames)
    dmtrevc = pd.read_csv(f'csv/{what}/DMTREVC.csv', header=None, names=colnames)

    vc['Clock'] = 'VC'
    evc['Clock'] = 'EVC'
    revc['Clock'] = 'REVC'
    dmtrevc['Clock'] = 'DMTREVC'

    df = pd.concat([vc, evc, revc, dmtrevc], axis=0, ignore_index=True)

    s = sns.lineplot(data=df, x=colnames[1], y=colnames[2], hue='Clock').set_title(title) #, err_style='bars')
    fig = s.get_figure()
    fig.savefig(f'plots/{what}.svg')
    plt.clf()
    plt.cla()

    return df


def print_stats(df, clock):
    print(f'\n{clock} clock stats:')
    print(df[df['Clock'] == clock].groupby('Number of processes')['Time (s)'].describe())


df = plot('time', 'Number of processes', 'Time (s)', 'Time of execution vs Number of processes, @95% conf. int.')
plot('bitsizes', 'Events', 'Size in bits', 'Size in bits vs Number of events per process, @95% conf. int.')

print_stats(df, 'VC')
print_stats(df, 'EVC')
print_stats(df, 'REVC')
print_stats(df, 'DMTREVC')
