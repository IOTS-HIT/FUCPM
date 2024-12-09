# FUCPM

This repo hosts the code for paper **"Utility-driven Mining of Contiguous Sequences"**, which was submitted to the Applied Soft Computing Journal.

### Requirements

Java programming language.

### Running the program

A simple way is to run the MainTestFUCPM.java.

### Benchmarks

- The data format used is the same as in the folder input.

### Introduction

Recently, contiguous sequential pattern mining (CSPM) gained interest as a research topic, due to its varied potential real-world applications, such as web log and biological sequence analysis. To date, studies on the CSPM problem remain in preliminary stages. Existing CSPM algorithms lack the efficiency to satisfy users’ needs and can still be improved in terms of run time and memory consumption. In addition, existing algorithms were developed to deal with simple sequence data, working with only one event at a time. Complex sequence data, which represent multiple events occurring simultaneously, are also commonly observed in real life. In this paper, we propose a novel algorithm, fast utility-driven contiguous sequential pattern mining (FUCPM), to address the CSPM problem. FUCPM adopts a com pact sequence information list and instance chain structures to store the necessary information of the database and candidate patterns. For further efficiency, we develop the global unpromising items pruning and local unpromising items pruning strategies, based on sequence-weighted utilization and item-extension utilization, to reduce the search space. Extensive experiments on real-world and synthetic datasets demonstrate that FUCPM outperforms the state-of-the-art algorithms and is scalable enough to handle complex sequence data.