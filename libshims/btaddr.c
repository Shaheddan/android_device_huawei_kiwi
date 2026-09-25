/*
 * libbtaddr.so only uses set_sched_policy() to adjust its own thread's
 * scheduling; a no-op that reports success lets it run, the same way the
 * tree's GPS code bypasses this call.
 */
int set_sched_policy(int tid, int policy) {
    (void)tid;
    (void)policy;
    return 0;
}
