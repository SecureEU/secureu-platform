import sys

def hello():
    return 'Method'

class Test():
    def on_init(self):
         return 'Class Method Constructor'

    def test():
        return  'Class Method'

if len(sys.argv)==4:
    methodName = sys.argv[1]
    sourceNf =  sys.argv[2]
    csvNf =  sys.argv[3]
    if methodName=='hello':
        print(hello())

    possibles = globals().copy()
    possibles.update(locals())
    method = possibles.get(methodName)
    if not method:
        raise NotImplementedError("Method %s not implemented" % methodName)

    print(method())

    print(methodName)
    print(sourceNf)
    print(csvNf)

