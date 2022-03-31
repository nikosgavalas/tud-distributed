---
title: |
    | Resettable Encoded Vector Clock
    | IN4391 Project Report
# names alphabetically
author: |
    | Group 5
    | Ana Oprea (5617294)
    | Jesse Harte (5637848)
    | Nikos Gavalas (5671477)
    | Thijs Verreck (4547381)
date: April 2022
link-citations: true
bibliography: bibliography.bib
csl: ieee.csl
---

# Introduction

A fundamental challenge in distributed systems is tracking the order of events occurring. In order to track ordering and causality relations, several ideas have been put forward by the scientific community over the years, such as Lamport’s _happened-before_ relation and logical clocks [@lamport].

Logical clocks and especially _vector clocks_ have since become essential in the design of distributed systems. However, they have an important limitation. Researchers, building upon the vector clock have proposed different variations to tackle these limitations, one of which is the Resettable Encoded Vector Clock (REVC) [@revc].

In this work, we present the REVC, and proceed afterwards to create an implementation of it and measure its properties in comparison to other types of vector clocks.

# The Resettable Encoded Vector Clock

<!-- What it is, Why use it (what problem it solves) -->

<!-- REVC internals, how it works -->

# Implementation

# Experimental evaluation

<!-- Experimental Setup -->

<!-- Results -->

# Discussion

# Conclusion

# References
