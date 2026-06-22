import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import io

'''
All datasets from scenarios 0-4
'''
data_s0 = """scenario,sim_crashes,entries,suc_entr,warnings,result
0,0,20,20,0,Success
0,0,20,20,0,Success
0,0,20,20,0,Success
0,0,20,20,0,Success
0,0,20,20,0,Success
"""

data_s1 = """scenario,sim_crashes,entries,suc_entr,warnings,result
1,1,20,18,1,Success
1,1,20,16,0,Success
1,1,20,17,0,Success
1,1,20,11,3,Success
1,1,20,19,0,Success
"""

data_s2 = """scenario,sim_crashes,entries,suc_entr,warnings,result
2,1,100,41,39,Success
2,1,100,34,49,Success
2,1,100,56,26,Success
2,1,100,50,30,Success
2,1,100,62,26,Success
"""

data_s3 = """scenario,sim_crashes,entries,suc_entr,warnings,result
3,2,100,24,56,Success
3,2,100,30,49,Success
3,2,100,47,31,Success
3,2,100,36,36,Success
3,2,100,39,35,Success
"""

data_s4 = """scenario,sim_crashes,entries,suc_entr,warnings,result
4,2.0,100,43,32,Success
4,2.0,100,47,39,Success
4,2.0,100,47,27,Success
4,2.0,100,55,23,Success
4,2.0,100,60,15,Success
"""

# Data into pandas dataframs
df0 = pd.read_csv(io.StringIO(data_s0))
df1 = pd.read_csv(io.StringIO(data_s1))
df2 = pd.read_csv(io.StringIO(data_s2))
df3 = pd.read_csv(io.StringIO(data_s3))
df4 = pd.read_csv(io.StringIO(data_s4))

# Merge df0-df4 to one df
df_all = pd.concat([df0, df1, df2, df3, df4], ignore_index=True)

# Success rate(s)
df_all['success_rate'] = (df_all['suc_entr'] / df_all['entries']) * 100
df_all['scenario'] = df_all['scenario'].astype('category')

print("DataFrame(s) with success rate:")
print(df_all)
print("\n--- Generating Plots ---")

sns.set_theme(style="whitegrid")


##########
# Plot 1 #
##########
plt.figure(figsize=(10, 6))
sns.boxplot(x='scenario', y='success_rate', data=df_all, palette="viridis")
sns.stripplot(x='scenario', y='success_rate', data=df_all, color=".3", size=4, alpha=0.5) # Show individual points
plt.title('Success rate per scenario')
plt.xlabel('Scenario (0-4)')
plt.ylabel('Success Rate(%)')
plt.ylim(0, 105)
plt.grid(axis='y', linestyle='--', alpha=0.7)
plt.tight_layout()
plt.show()


##########
# Plot 2 #
##########
plt.figure(figsize=(10, 6))
sns.barplot(x='scenario', y='success_rate', data=df_all, palette="viridis", capsize=.2, errorbar='sd')
plt.title('Average success rate per scenario')
plt.xlabel('Scenario (0-4)')
plt.ylabel('Average Success Rate(%)')
plt.ylim(0, 105)
plt.grid(axis='y', linestyle='--', alpha=0.7)
plt.tight_layout()
plt.show()
