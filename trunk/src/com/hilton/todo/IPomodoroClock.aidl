package com.hilton.todo;

interface IPomodoroClock {
    int getRemainingTimeInSeconds();
    void cancelClock();
}