import sys

print("Hello World!!")

print("interfaces:")
for i, interface in enumerate(interfaces):
        print("interface " + str(i) + ":" + str(interface) )


print("Arguments count:" + str(len(sys.argv)))
print ("Argument List:", str(sys.argv))
for i, arg in enumerate(sys.argv):
        print("Argument" + arg )


def hello():
    return 'Method'

class Test():
    def on_init(self):
         return 'Class Method'