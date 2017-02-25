fun rl_encode [] = []
  | rl_encode (x::xs) = 
     let fun enc (y, [], acc) = [(y,acc)]
	       | enc (y, x::xs, acc) = if x=y
		                           then enc(y, xs,acc+1)
								   else (y,acc):: (enc(x, xs, 1))
	 in enc(x, xs, 1) end;
	 
fun length (x::xs) = 1+length xs
  | length _ = 0;

fun genEquals (~1) _ _ = false
  | genEquals _ [] [] = true
  | genEquals n [] ys = if length ys > n then false else true
  | genEquals n xs [] = if length xs > n then false else true
  | genEquals n (x::xs) (y::ys) = if x=y then genEquals n xs ys
                                  else ((genEquals (n-1) (x::xs) (ys)) orelse (genEquals (n-1) xs (y::ys)) orelse (genEquals (n-1) xs ys));