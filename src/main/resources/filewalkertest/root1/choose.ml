fun cons x xs = x::xs; 
fun length (x::xs)= 1+ length xs | length _ = 0;
fun choose (0, _) = [[]]
  | choose (k, []) = [[]]
  | choose (k, xs) =  let fun choosehelp (0, _, _ ) = []
                           | choosehelp (1, x::xs, ys) = [x] :: choosehelp(1,xs, x::ys)
                           | choosehelp (k, [], _) = []
                           | choosehelp (k, x::xs, ys) = (map (cons x) (choose(k-1, xs)))@ choosehelp(k, xs, x::ys)
                     in if length xs = k then [xs] else choosehelp(k, xs, []) end;