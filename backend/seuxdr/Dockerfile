FROM redhat/ubi9

# Set the hostname
RUN echo "Mac.home" > /etc/hostname

# Install required packages
# Set Go version
ENV GO_VERSION=1.22.5

# Install dependencies and Go manually
RUN yum update -y && \
    yum install -y wget tar sudo perl python3 && \
    wget https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz && \
    rm -rf /usr/local/go && \
    tar -C /usr/local -xzf go${GO_VERSION}.linux-amd64.tar.gz && \
    rm go${GO_VERSION}.linux-amd64.tar.gz
        
# Install Go manually
RUN curl -LO https://go.dev/dl/go1.24.2.linux-amd64.tar.gz && \
rm -rf /usr/local/go && \
tar -C /usr/local -xzf go1.24.2.linux-amd64.tar.gz && \
rm go1.24.2.linux-amd64.tar.gz

# Set PATH
ENV PATH="/usr/local/go/bin:${PATH}"


# Install systemd and dependencies
RUN yum install -y systemd && \
    yum clean all && \
    (cd /lib/systemd/system/sysinit.target.wants/; \
    rm -f $(ls | grep -v systemd-tmpfiles-setup)) && \
    rm -f /lib/systemd/system/multi-user.target.wants/* \
          /etc/systemd/system/*.wants/* \
          /lib/systemd/system/local-fs.target.wants/* \
          /lib/systemd/system/sockets.target.wants/*udev* \
          /lib/systemd/system/sockets.target.wants/*initctl* \
          /lib/systemd/system/basic.target.wants/* \
          /lib/systemd/system/anaconda.target.wants/*


# Copy the parent directory of the current directory
COPY . /seuxdr

# Pre-build the SEUXDR Go server so that runtime restarts are instant.
# Without this, start-server.sh runs `go run main.go` on every restart and
# recompiles SQLite C bindings (~3 min per restart). The binary is written to
# /seuxdr/manager/bin/seuxdr-manager and start-server.sh execs it.
RUN cd /seuxdr/manager && \
    /usr/local/go/bin/go mod download && \
    mkdir -p bin && \
    CGO_ENABLED=1 /usr/local/go/bin/go build -trimpath -ldflags='-s -w' -o /seuxdr/manager/bin/seuxdr-manager . && \
    chmod +x /seuxdr/manager/bin/seuxdr-manager

# Set correct permissions for Go execution and fix PAM for sudo in container
RUN usermod -aG wheel root && echo 'root ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers && \
    # Fix PAM configuration for sudo to work in container
    sed -i 's/^account.*required.*pam_unix.so$/account    sufficient   pam_unix.so/' /etc/pam.d/sudo && \
    sed -i 's/^account.*include.*system-auth$/account    sufficient   pam_permit.so/' /etc/pam.d/sudo


# Copy a startup script
COPY startup.sh /usr/local/bin/startup.sh
RUN chmod +x /usr/local/bin/startup.sh

# Set the working directory to /seuxdr/manager
WORKDIR /seuxdr/manager

# Expose port 443
EXPOSE 8443
EXPOSE 8081

# Start systemd
CMD ["/usr/sbin/init"]
