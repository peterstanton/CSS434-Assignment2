# .bash_profile

# Get the aliases and functions
if [ -f ~/.bashrc ]; then
        . ~/.bashrc
fi

# User specific environment and startup programs

# Specific to MPICH
#PATH=$PATH:/usr/local/mpich2/bin

export PATH=/usr/apps/mpich121-`uname -p`/bin:$PATH

# Specific to mpiJava
# PATH=.:$PATH:~css434/mpiJava/src/scripts:$HOME/bin # mpiJava covered below.
PATH=.:$PATH:$HOME/bin
export PATH

#
export PATH=/usr/apps/mpich121-`uname -p`/bin:$PATH
export JAVAPATH=/usr/lib/jvm/default-java
export CLASSPATH=$CLASSPATH:/usr/apps/mpiJava-`uname -p`/lib/classes:.
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/apps/mpiJava-`uname -p`/lib
export PATH=/usr/apps/mpiJava-`uname -p`/src/scripts:$JAVAPATH/bin:$PATH

unset USERNAME


