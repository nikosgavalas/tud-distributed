import matplotlib.pyplot as plt
import numpy as np
import csv

def load(path):
    with open(path, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        # headers = next(reader)
        return np.array(list(reader)).astype(float)

vc = load('csv/VC.csv')
evc = load('csv/EVC.csv')
revc = load('csv/REVC.csv')
dmtrevc = load('csv/DMTREVC.csv')

time = vc[:, 0]

fig, ax1 = plt.subplots()

color = ''
ax1.set_xlabel('Number of processes')
ax1.set_ylabel('Time (s)')
ax1.plot(time, vc[:, 1], label='VC')
ax1.plot(time, evc[:, 1], label='EVC')
ax1.plot(time, revc[:, 1], label='REVC')
ax1.plot(time, dmtrevc[:, 1], label='DMTREVC')
ax1.tick_params(axis='y')
ax1.legend()

# ax2 = ax1.twinx()
#
# color = 'tab:blue'
# ax2.set_ylabel('Unique Errors', color=color)  # we already handled the x-label with ax1
# ax2.plot(random_iter, random_errors, color=color, label='random')
# ax2.tick_params(axis='y', labelcolor=color)
# color = 'tab:green'
# ax2.plot(symbolic_iter, symbolic_errors, color=color, label='symbolic')
# ax2.legend(loc='lower right')

plt.title("Clock's execution time vs number of processes")
plt.show()
