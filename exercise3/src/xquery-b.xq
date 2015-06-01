declare namespace saxon = "http://saxon.sf.net/";
declare option saxon:output "indent=yes";

<stats>
{
    for $game in (//game)
    let $sessionid := $game/@session
    return
        <session id="{$sessionid}">
        {
            for $player in data($game/player/@ref)
            let $output :=
                for $quID in $game/asked/@question
                for $givenanswer in $game/asked/givenanswer[@player=$player]
                let $correct := (//question[@id=$quID]/answer[@correct="yes"]/text())
                where $correct = data($givenanswer)
                   return
                     <givenanswer player="{$player}">{data($givenanswer)}</givenanswer>
            return
               <correct player="{$player}">{count($output)}</correct>
        }
        </session>
}
</stats>
