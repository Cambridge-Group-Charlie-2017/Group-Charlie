fun depth (lim, start, Puzz(nsf, win)) =  let fun path ([],_,_) = false
										       | path (_, 0, _) = false
											   | path (x::xs, n, Puzz(nsf, win))= if win x then true     
											                                               else if path (nsf x, n-1, Puzz(nsf,win)) then true 
																						                                            else path (xs, n, Puzz(nsf,win))
									     in if win start then true else path(nsf start, lim, Puzz(nsf,win)) end;