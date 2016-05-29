<p align="center">Large-Scale Distributed Systems</br>Group Messenger with TOTAL and FIFO ordering</br>modified ISIS algorithm</br>CSE 586
==========================================================================================


Goal
------
Implement advanced concepts that add ordering guarantees to the group messaging. Specifically, we need to develop a **Group Messaging Android application with decentralized [TOTAL and FIFO ordering](https://www.youtube.com/watch?v=qhL7GW1KOj8) guarantees**. 

**NOTE**</br>
[**ISIS system**](http://webcache.googleusercontent.com/search?q=cache:3rOwsftQvYoJ:www.cs.cornell.edu/home/rvr/sys/p79-birman.pdf+&cd=1&hl=en&ct=clnk&gl=us&client=safari) developed at Cornell (Birman, 1993; Birman and Joseph, 1987a, 1987b; and Birman and Van Renesse, 1994) provides totally ordered multicast delivery algorithm. However in this assignment we need to design and **implement a modified version of ISIS algorithm** that guarantees both TOTAL and FIFO ordering and provides a persistent Key-Value storage with **ordering remaining intact even in case of application failures**.

<p align="center">![ISIS_Working](https://github.com/ramanpreet1990/CSE_586_Group_Messenger_TOTAL_FIFO_Ordering/blob/master/Resources/ISIS_Algorithm_Working.gif) 


References
---------------
References used to design a modified version of ISIS algorithm: -</br>
1. [Lecture slides](http://www.cse.buffalo.edu/~stevko/courses/cse486/spring16/lectures/12-multicast2.pdf)</br>
2. [Distributed Systems: Concepts and Design (5th Edition) ](https://www.pearsonhighered.com/program/Coulouris-Distributed-Systems-Concepts-and-Design-5th-Edition/PGM85317.html)
3. [Cloud Computing Concepts - University of Illinois at Urbana-Champaign](https://www.coursera.org/learn/cloud-computing)


What is FIFO Ordering
-------------------------------
The message delivery order at each process should **preserve the message sending order** from every process. But **each process can deliver in a different order**.

For example: -
–  **P1:** m0, m1, m2
–  **P2:** m3, m4, m5
–  **P3:** m6, m7, m8

One of the FIFO ordering would be: - 
–  **P1:** m0, m3, m6, m1, m2, m4, m7, m5, m8
–  **P2:** m3, m0, m1, m4, m6, m7, m5, m2, m8
–  **P3:** m6, m7, m8, m0, m1, m2, m3, m4, m5


What is TOTAL Ordering
----------------------------------
**Every process delivers all messages in the same order**. Here we don't care about any causal relationship of messages and as long as every process follows a single order we are fine.

For example: -
–  **P1:** m0, m1, m2
–  **P2:** m3, m4, m5
–  **P3:** m6, m7, m8

One of the TOTAL ordering would be: - 
–  **P1:** m8, m1, m2, m4, m3, m5, m6, m0, m7
–  **P2:** m8, m1, m2, m4, m3, m5, m6, m0, m7
–  **P3:** m8, m1, m2, m4, m3, m5, m6, m0, m7


What is TOTAL and FIFO Ordering
-----------------------------------------------
The message delivery order at each process should **preserve the message sending order** from every process and  **every process delivers all messages in the same order**.

For example: -
–  **P1:** m0, m1, m2
–  **P2:** m3, m4, m5
–  **P3:** m6, m7, m8

One of the TOTAL and FIFO ordering would be: - 
–  **P1:** m0, m3, m6, m1, m2, m4, m7, m5, m8
–  **P2:** m0, m3, m6, m1, m2, m4, m7, m5, m8
–  **P3:** m0, m3, m6, m1, m2, m4, m7, m5, m8
