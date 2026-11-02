const {test,before,after}=require('node:test');const fs=require('node:fs');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,getDoc,setDoc}=require('firebase/firestore');
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-layali-rules',firestore:{host:'127.0.0.1',port:8085,rules:fs.readFileSync('../firestore.rules','utf8')}});await env.withSecurityRulesDisabled(async ctx=>{const db=ctx.firestore();await setDoc(doc(db,'accounts/a'),{status:'active'});await setDoc(doc(db,'accounts/b'),{status:'banned'});await setDoc(doc(db,'wallets/a'),{balanceCents:100});await setDoc(doc(db,'wallets/b'),{balanceCents:100});});});
after(async()=>env?.cleanup());
test('owner may observe balance; another user and guests cannot',async()=>{await assertSucceeds(getDoc(doc(env.authenticatedContext('a').firestore(),'wallets/a')));await assertFails(getDoc(doc(env.authenticatedContext('b').firestore(),'wallets/a')));await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'wallets/a')));});
test('client cannot edit balances, roles, payouts or legacy wallet',async()=>{const db=env.authenticatedContext('a',{admin:true}).firestore();for(const path of ['wallets/a','creatorBalances/a','accounts/a','withdrawals/fake','ledger/fake','users/a'])await assertFails(setDoc(doc(db,path),{balanceCents:1000000,role:'admin'}));});
test('banned user cannot read wallet but can receive account status',async()=>{const db=env.authenticatedContext('b').firestore();await assertFails(getDoc(doc(db,'wallets/b')));await assertSucceeds(getDoc(doc(db,'accounts/b')));});
