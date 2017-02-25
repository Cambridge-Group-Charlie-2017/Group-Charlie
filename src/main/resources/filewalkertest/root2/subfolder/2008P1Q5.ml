datatype 'a lazytree = Lf | Br of 'a * ( unit -> 'a lazytree) *(unit -> 'a lazytree);
datatype 'a seq = Nil | Cons of 'a * (unit -> 'a seq);

fun intcreate n = Br(n, fn()=> intcreate(2*n), fn() => intcreate(2*n+1));
fun intcreaten n = Br(n, fn()=> intcreaten(2*n), fn() => intcreaten(2*n-1));
val allint = Br(0, fn()=> intcreate(1), fn() => intcreaten (~1));

fun next (Cons(x,xf))= xf();

fun treetolist (Br(x,t1,t2))=
            let  fun help [] = Nil
			       | help ((Br(y,r1,r2))::ys) = Cons(y, fn() => help (ys@[r1()]@[r2()]))
			in Cons(x, fn() => help(t1()::[t2()])) end;
			
treetolist allint;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;
next it;