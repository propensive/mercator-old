/*
  
  Mercator, version 0.1.1. Copyright 2018 Jon Pretty, Propensive Ltd.

  The primary distribution site is: https://propensive.com/

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
  this file except in compliance with the License. You may obtain a copy of the
  License at
  
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.

*/
package mercator

import scala.annotation.compileTimeOnly
import scala.language.higherKinds
import scala.reflect.macros._

import language.experimental.macros

object `package` {
  implicit def monadicEvidence[F[_]]: Monadic[F] =
    macro Mercator.instantiate[F[Nothing]]
  
  final implicit class Ops[M[_], A](val value: M[A]) extends AnyVal {
    @inline def flatMap[B](fn: A => M[B])(implicit monadic: Monadic[M]): M[B] =
      monadic.flatMap[A, B](value, fn)

    @inline def map[B](fn: A => B)(implicit monadic: Monadic[M]): M[B] =
      monadic.map[A, B](value, fn)
    
    @inline def withFilter[B](fn: A => Boolean)(implicit monadic: MonadicFilter[M]): M[A] =
      monadic.filter[A](value)(fn)
  }
}

object Mercator {
  def instantiate[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val typeConstructor = weakTypeOf[F].typeConstructor
    if(typeConstructor.toString.endsWith("Set")) c.warning(c.enclosingPosition, tpolecat)
    val mockType = appliedType(typeConstructor, typeOf[Mercator.type])
    val companion = typeConstructor.dealias.typeSymbol.companion
    val returnType = scala.util.Try(c.typecheck(q"$companion.apply(_root_.mercator.Mercator)").tpe)
    val pointApplication = if(returnType.map(_ <:< mockType).getOrElse(false)) q"${companion.asModule}(value)" else {
      val subtypes = typeConstructor.typeSymbol.asClass.knownDirectSubclasses.filter { sub =>
        c.typecheck(q"${sub.companion}.apply(_root_.mercator.Mercator)").tpe <:< mockType
      }.map { sub => q"${sub.companion}.apply(value)" }
      if(subtypes.size == 1) subtypes.head
      else c.abort(c.enclosingPosition, s"mercator: unable to derive Monadic instance for type constructor $typeConstructor")
    }

    val filterMethods: List[Tree] = if(mockType.typeSymbol.info.member(TermName("filter")) == NoSymbol) Nil
      else List(q"def filter[A](value: Monad[A])(fn: A => Boolean) = value.filter(fn)")

    val instantiation =
      if(filterMethods.isEmpty) tq"_root_.mercator.Monadic[$typeConstructor]"
      else tq"_root_.mercator.MonadicFilter[$typeConstructor]"

    q"""
      new $instantiation {
        def point[A](value: A): Monad[A] = $pointApplication
        def flatMap[A, B](from: Monad[A], fn: A => Monad[B]): Monad[B] = from.flatMap(fn)
        def map[A, B](from: Monad[A], fn: A => B): Monad[B] = from.map(fn)
        ..$filterMethods
      }
    """
  }

  def tpolecat = """
                                                                                                                                                      
                                                                      ..'',,,,,'...                                                                   
                                                                .';coxO0KXXXXXXKK0Oxdl:,.                                                             
                                                            .';oxOKXXNNNNNNNNNNNNNNNNNXX0ko;.                                                         
                                                         .,cxOKXXXXNNNNNNNNNNNNNNNNNNNNNNNNX0xc,.                                                     
                                                      .':ok0KXXNXXXXXNNNNNNNNNNNNNNNNNNNNNNXXXK0xl,.                                                  
                                                    .,cdkO000KKKKXXXXXXXXXXXXNNNNNNNNNNNNNNXXXXKKK0kdlc'                                              
                                                  .,codxkkkkkkO00KKKKXXKKKKKKKXXXNNNNNNNNNNNNXXXXXXNNNX0c.                                            
                                                 .;cooddddddddxkkkOO0KKK000000KKXXNNNNNNNNNNNNNNNNXXXK0K0xlldxo,                                      
                                               .':clloolllllloddddxxkOOO000000000KKXNNNNNNNNNNNNNNX0Okk0XXXNNNNk.                                     
                                              .':ccccllllllllooloodddxxkkkkkkkOOOO00XNNNNXXNNNNNNKOdookKNNNNNNKl.                                     
                                             .,;::cccccclllllllcllolloddddxxxxxxxkkO0KXXXXXXXXNX0kddkKNNNNWNX00klll;.                                 
                                            .,;;::::::::cccllooollcllloooodddddddxxkO0KXXNNNNXKkookKXNNNNWNXOxOKXNNK:                                 
                                           .',;;;;;;;;::::codxxxxxooccllllooooooddddk0KXNNWNNKxld0XNNNNNNNKxdxKNNNN0;                                 
                                          .';;;;;;;;;;;;;;:oddxxkOkxdlcclllllooooooxOKXXNNNNKxldKXNNWWWNKOdokXNWNNXOocl:.                             
                                          .,;;;;;;;;;;;;;;:ccclodkO0OkoccccllllllldOXXNNNNNKkoxKNNNWWNX0xox0NNWWNKO00KK0:                             
                                          .,;;;;,,,,;;;;;;;;,;:loxO000kl:;:ccclllokKNNNNNNNK00XNNWWNNXOddOXNNNXXKkOKXNX0c                             
                                         .';:;;;;;,;;;;;;;,,,,;ldxkOO00x:;;:::ccdOKXNNNNNNNNNNNNWWNNKkokKNNNNXX0kx0XNXKk,                             
                                         .';;;;,,,;;;;;;;;;,,,,cdxkkO00Oo;;;;:cd0KKKXNNNNNNNNNNWWWWN0k0NNNWNNX0kk0XNNX0x'                             
                                         .';;;,,,,;;;;;;;;,,,,,:dkkO0000x:,;;cxO0O0KXNNNNNXXKXNNWWNNXNNNNNNNKkxOXNNNXKOOc                             
                                         .';;;,,;;;;;;,;;,,,,,,;lkO00KK0klcloO00OOO0KXXNXXKKKKXNNNNNNWWWNNX0xx0XXXKK0OOOo.                            
                                         .,;;;,;;;;,;;,,,,,,,,,,cxO0KXK0OOO0KXXK0OO0KXXXKK000KXXNNNNNWWWNN0xkKNNNK0kkOOOd.                            
                                         .,;:;;;;;;,,,,,,,,,,,,,:x0KXXKKKXXXXXXX00KKKKKKKKKKKKXXNNNNNNNWNX00XNNNX0kkOOOOd'                            
                                         .,::;;;;;;;;,,;;;;;,,,,cx0KXXXXXKKKKXXK0000KKKKK0KKXXXXNNXXXNNNNXKXNXXK0OkkOkkOx,                            
                                         .';:;;;;;;;;;;;;;;;;,,;lx0KKKKKK0000KKK000000KKK0KKKXKKKXXXXXNNXXXNNXX0OkkOOOO0k;                            
                                         ..,;;;;;:looolc;;;;;;,;lxO0000000O000K00000KKKKKKKKKXXXXKXKXXXXXXXNNNX0kxkO0000Oc                            
                                          .,;;;:lddooddooc:;;,,;cdxkOOOOOOOO00000000KKKKKKXKKKXNXXXXXXXXXXXXNXKOkkk0KKKK0o.                           
                                          .,;:codl:::ccccc::;,,;:odxkkOOOkkkOO000KK0KKKKKKXKK0KNNNXXXXXXXXXXXKOkkkO0KXKK0d.                           
                                          .,::lol:::lll:;;;::;;;;codxkkkkkxkkO000000KKKXXXXXXKKXNXXKXNNNNNNXKOkkkkO0KK00Ol.                           
                                           ';;:lc;:lolc::;;;::;;;:lddxkkkkkkkOO0000KKKKXXXXNNXKXXXXXNNNNNNXK0kxkkkOO00Oxl.                            
                                           .,;:cc:clc;;;,,',;:;;;;:ldxkOOkkkkOOO00KKKKKXXXXNXXXXXXNNNNNNNXXKkxxxxkkkkd:.                              
                                           .;::cc:cc;,;:;;,,,;ccc::codkO00OOOOOOO000KXXXXXXXXXXXXNNNNNXXXK0koccllooo:.                                
                                           .;::clc:::;;;,,',,:llcc::codk0K00OOOOO000KKXXXKKKKKXXXNNNXXKK0Oxl:::clod:.                                 
                                          .,:cccllc::::cc::;;clccc:;;cldOKK000OOO00KKXXXXKKKKKXNNXXXK000Odc:cccldxkl.                                 
                                          'clccllolcclloool:;:c::c:;;;cok0KK000O000KKXXXXXXXXXNXXXXK000kdlloollloxOx'                                 
                                         'cllcclooolloddoolc:::::::;,,;lx0KKKK000KKKXXXNXXXNNNNNXXK000OdlldxdoooodkOd.                                
                                       .'codollllollllooollcc:::::;,,,;ok0KXKKK0KKKKXXNNNNNNNNNNNXXK0kdoodxxxdddddk00l.                               
                                   ..',;:loooollolllcccclccc::;;:;,,,;cxO0KKKK000KKKXXXXNNNNNNNNNXX0kdodxkkxddoodxO0K0o.                              
                             ...,;cllcc::clodoooolllccccc:::ccc:;;,,,:okO00K000000KKKKXNNNNNWWNNNX0kxdxkkxdllooooxO0KKKo.                             
                      ....,:loddxxxddl:;,,:oddddolllccccccccllc:;;,,,cxOOO000000000KKKXXXXNNWWNNX0kxxxxkxl:clddxxxxkOOOo.                             
                  ..'',,'';okOOOOOkxdo:,'',coodoollcccccccccccc:;;,,;okO00000000KKK00KKXXXXNNNNNXOxxxxxxoc;::cclllllcc;.                              
              ..',,,,,'''.',cxO00OOkdol:,'',:looollcccccccccc::;,,,,:dOO00000KKKKKKK00KKKKXXXNNNKkdxxxddl:;,,,;;::c;.                                 
           .',,,,,,,,''''...',cdkOOOkddo:,'',:lllllcccccc::::;;,''',:dOO0000KKKKKKKKKKKKKKXXXXXNKxdddolc:;,,,,;:::;,.                                 
       ...'''''...........''...';cdkkxxdoc,'',;clllcccc:::::;,,,''',cxO000KKKKKKKXXXKKKKXXXXXXXX0xooc:;;;,,;;;;;,.                                    
   ..',,,,,'''''''...........'....';coxxxdc;'',;cllcc::::::;;,''..',cxO00KKKKKKXXXXXXKXXXNNNXXNX0dc:;,;;,,,,;;::'                                     
..,;;;;;;;;;;;;,,'''.................',:lodl;,'',:ccc:::::;;;,'..'',lk000KKKKKKXXXXXXXXNNNNNXXXXOc;;;;,,,,;:llc;.                                     
:cc:::::;;;;;;;,,,,,''''.................',:c:,'',;ccc:::::;;;,'..';okO00KKKKKKKKKKKXXNNNNNNNNNXk:;:c:cccloodo:.                                      
::;;;;;;;;;;;;,,,,;;,,'''''...................''''';:ccc:::;;;,,'.':dkO000000KKKKKKXXXNNNNNNNNNNOl:clccloollolc.                                      
::;;;:::::;;;;;;;;,,,,,,,,,,''....... ......   ..''',:ccc::;;;;,'.'cxOO0000000KKKKKXXXNNNNNNNNNN0o:c:::clcccllc.                                      
,,,,,,;,,''',,,,,,,,,,,;;;,,,,,'.......          ..'',;::c::;;;,,',lkOO000000KKKKKKXXXNNNNNNNNNNKd::::::ccclllc.                                      
,''...................',,;;;,,,,,''..'....         .''''',::;;;;,,;okOOO000KKKKKKKXXNNNNNNNNNXXXXx:;;;::clllllc.                                      
'...'''''''.........   ...'',,,,,,,''..'......      ..''...',;;;,,:dkkOO00KKKKKKKKXNNNNNNNNNNNXXXOc,,;:::ccccc'                                       
'''''''',''''''''''''...   ...''''',,''..'.......     ..''.....'',cdkOO00KKKK0KKKKXXNNNNNNNNNNNXX0l;,,,;;;:cl:.                                       
''''''.............................'',,''..''.......   ..',..   .'lxkOO0000000KKXXXXNNNNNNNNNNNXXKd;,,,,;::clc.                                       
'...'''''''''''.......................'',,''''........  ..','....;okkOO0000000KXXXXXNNNNNNNNXXNXNXx:,,;;:cc:cc,.                                      
,,,''''''''.''''.........................,,,''''............,,..,cxkOOOO00000KKXXXXNNNNNNNNNNNXXXXk:,,;:ccc:::;'                                      
,,,,,,,,,,,,,,,,,'''''....................',,,'''............,,,:okkOO000KK0KKXXXXNNNNNNNNNNNNXXXXOc,;:cclc;,,,,.                                     
,,,,,,,,,,,,,,,,'''''''''''''''.............''''''............,;lxkOO0000KKKKKXXXXNNXNNNNNNNNNXXXX0l;;:ccc:;..',,.                                    
,,,,,,,,,,,,,,,,,,,,,''''''''''''''''.........'''''...........'cdkOkO0000000KKXXXXXXXNNNNNNNNNXXXX0o;::;;;;;'..','.                                   
,,,,,,;;;,,,,'''''...................'''....  ..'''''.... ....;okkOOO00000000KKXXXXXXXXXXNNNXXXXXXKo,,;,,,;;,...,,.                                   
,,,,,''''..................''''''''''''''''...  .'''''.......,lxkkOOO000000000KKXXXXXXXXNNNNXXXXXXKd,''',,,;,'..','.                                  
,'''..''''''''''''''''''''''''',,,,,,,,,,,,,,'.. ..'''......'cdkOOOOO00000KKKKKKXXXXXXXXNNNNXXXXXXKx;.'',,,;;,..''',.                                 
'''''''''''''''''''''''''''',,,,,,,,,,,,,,,,,,,'......'.....;oxkOOOO00000KKKKKXXXNXXXXXXXNNNXXXXXXXk:''',,,;;;:cc:,''.                                
''''''''''''''''..'''''''',,,,,''''''''',,,,,,,,,,''.......,lxkkOOOO00000KKKKKXXXNNNNXXXXXXXXXXXXXKOc'''',,,;cxO0x:''..                               
....''...'''''''''''''''''''...''''''',,,,,,,,,,,,,,,.....,cdkkkOOO000000KKKKKKXXXXNNXXXXXXXXXXXXKK0l'..'',,;coxOd;'''.                               
.''''''''',,''''''............'',,,,,,,,,,,,,,,,,,,,,,'.',cdxkOOOO0000000KKKKKKXXXXNNXXXXXXXXXXXKKK0o,..'',,;:loxx:''''.                              
''''''''..................''',,,,,,,,,,,,,,,,,,,,,,,,,,',:dxkOOOOO0000000KKKKKKXXXXXXXXXXXXXXXXXKKK0o,..'''',;coxkxc'.''.                             
.......... ...........''',,,,,,'',,,,,,,,,,,,,,,,'''''';cdkkOOOOO00000000KKKKKKXXXXXXXXXXXXXXXXXKKK0d,..''''',:ldkOkc'.',.                            
..................''',,''''.''',,,,,,,,,,,,,,,,,''''',;cdkkkOOOOO000000000KKKKKXXXXXXXXXXXXXXXXXKKK0d,....',,'';ldkOkl,'''.                           
................'','''...'',,,,,,,,,,,,,,,,,,,,,,,;;;:lxkkkkOOOOOO0000000000KKKKXXXXXXXXXXXXXXXXKKK0d,.....','',:ldkOOx:,''.                          
....................''',,;;;,;;,,,,,,,,,,;;:clodddxdooxkkOOOOOOOOO00000000000KKKXXXXXXXXXXXXXXXKKK00d,......''',:cldkO0kl,''.                         
.................',,,;;,,,,,,,,,,,,,,;;cldkO0000K0OkxxkkOOOOOOOOOO0000000000000KKXXXXXXXXXXXXXKKKK00x;.... .....;llokOOOkc''.                         
,,,,,,''',,,,,,,''''''..''',,,,,,,;:ldkO0KXXKKKKK0OkkkkOOOOOOOOOO000000000000000KKXXXXXXXXXXXKKKKKK0x;........',cdxxkOOOOd;.                          
.......................',,,,,,,;:ldO0KKXXXKKKKKK0OkkkkkOOOOOOOO0OO000000000000000KXXXXXXXXXKKXKKKKK0d:;:cloddxkOO00000OOOkd,                          
....................',,,,,,,,:ldO0KXKKKKKKKKKKK0OkkkkOOOOOO00OOOOO00000000OOO0000KXXXXXXXXXKXXXXKK0OxodxkxxkOO0KK000KKKK00ko'                         
.................'',,,,,,,;:ok0KKKKKKKKKKKKKKKKOkxkkOOOOOOOO0OOOOO00000000OOO0000KXXXXXXKKKKXXXKKK0kdoodoooxkkOOOkOOOOOOOOkxc.                        
...............',,,,,,,,;:ok00KKKKKKKKKKKKKKK00xdxkkOOOOOOOOOOOOOO00000000OOOO000KKXXXXXKKKKKKKKKK0xlcccc::llooooooodxkkkxddl.                        
..............',,,,,,,,:oxO0000KKKKKKK000KKK00kddxxkkkkOOOOOOOkOOOOOOOOOOOOOOOO00KKKXXXKKKKKKKKKK0Oo::::cloddodddxxkO0000Okxd;                        
............',,',,,,,;lxkOOkkOO0000000000000OOxddxxxxkkOOOOOOkOOOOOOOOOOOOOOOOO0KKKXXXKKKKKKKKKKK0kdooooxO0KKXXXXKKKKXXXXKKkdo,                       
'''''''''',,,,,,,'',:oxkkkkxxkkOOOOO0OOOOOOOOkooxxxxxxkOOOOOOOOOOOOOOOOOOOOOOOO0KXXXXKKKXKKKKKKK0OxllcccoxOOOOOOOOOOOOO0000kdoo'                      
''''',,,,,,,,,,,'';ldxkxxxxxxkkOOOOOOOOOOOOOOxloxxxxxxkkkkkkkOOOOOOOOOOOOOOOOOO0KXXKKKXXXXKKKKK00Oo;;;;;:coodddooooddddddddooodc.                     
''''',,,,,;;,,'',:ldxxxxxxxxxkkkkkkkkkkOOOOOOdlodxxxxxxxkkkkkkkOOOOOOOOOOOOOOO00KKKKKXXNNXXKKKK00x:..'''''',,;;;:clooooolccllood:                     
...',,,,;;;,'..,:loddxxddxxxxxxxkkkkkkkkkkkOkddddddxxxxxxkkkkkkkkOkkkkkkkkOOOO00000KKXXNXXKKKKK0Oo:;ccc::lodddooxO000OOkkkolcldxc.                    
.'''',,,;,'...,;clooddddddddxxxxxxxxxxxxxxxkkxxdooddddxxxxkkkkkkkkkkkkkkkkOOO0000000KXXXXKKKKK0Ox:,:looooxOKKKK0000OOOOO0Ooccclo:.                    
',,,,,,'..   .';ccllooddooddddxxxxxxxxxxxxxxxxxdooodddddxxxxkkkkkkkkkkkkkOOOO00000000KXXKKKKKK0kc...',;::codxxdooooooooolllccccc;.                    
,,,,,'.        .,:ccloooooddddddddddddddddddxxddddooddddxxxxxxxxxkkkkkOOOOO0000000000KKKKKK000Od' ..'''',,;:::;;:cclodddolccccc:'                     
;;,'..           .';cllooooooodooooddddoooooodddddooddddxxxxxxxxxxkkkkkOOO00000000000KKKKKK00Okc.  ':ccc:::loodxkkkkOkkxoc:;;::,.                     
,;.                .';:llllooooollooooooolllloooddooooddxxxxxxxxxxxxxxkkO0000000000000000000Oko,.  .,:clccoxOOOOOkxxdoll:;,,,,'.                      
'.                   ..,:ccllllllllllllllllllllooooloooddxxxxxxxxxxxxxkkO000000000000000000OOd:'........',;:cccc:;;;;;;;;;;;'.                        
..                     ..,::cccccccccllccllllllllllllloodxxxxxxxxxxddxxkOO00000K00000000000Okl'.......    ..........'',,;;,.                          
.........                ..';:::ccccccccccccccccclllllloodxxxxxxxxxxddxkkOO0000K00000000000Ox:'....'..          .....',,,'.                           
....      .                 ..,;::cccccccccccccccclllllloodddxxxxxxxdddxxkOOO0000000000000Oko;'....''.      .........''..                             
        ...                   ...',;::cccc:cc:::::ccccllllloddxxxxxxddxxxxkOOO0000OO00000OOxc,,'....'.................                                
   ........                    .. ...,;;:::::::::::::ccccclllooddxdddxxxxxkkkkOOOOOO000000Od;'','...'..............                                   
..........                    ..     ..',;;:::;::::::::cccccclloodddddxxxxxxxkkkkkOOOOOOOOOo,''',...',.............                                   
"""
}

trait Monadic[F[_]] {
  type Monad[T] = F[T]
  def point[A](value: A): F[A]
  def flatMap[A, B](from: F[A], fn: A => F[B]): F[B]
  def map[A, B](from: F[A], fn: A => B): F[B]
}

trait MonadicFilter[F[_]] extends Monadic[F] {
  def filter[A](value: F[A])(fn: A => Boolean): F[A]
}
